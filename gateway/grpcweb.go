package main

// grpc-web 프록시.
//
// Envoy 의 grpc_web 필터가 하는 일을 최소 구현으로 드러낸다. Envoy 설정은 envoy/envoy.yaml
// 에 있고 실무에서는 그쪽이 표준이지만, 여기서는 프레임 구조를 코드로 보이는 것이 목적이다.
//
// grpc-web 과 gRPC 의 와이어 포맷은 거의 같다. 둘 다 길이 접두 프레임을 쓴다.
//
//	[1바이트 flag][4바이트 big-endian 길이][payload]
//
// flag 가 0x00 이면 데이터 프레임, 0x80 이면 trailer 프레임이다. 차이는 두 가지다.
//
//  1. gRPC 는 HTTP/2 를 요구하지만 grpc-web 은 HTTP/1.1 로도 동작한다.
//  2. gRPC 는 상태를 HTTP/2 trailer 에 담는데, 브라우저는 trailer 를 읽을 수 없다.
//     그래서 grpc-web 은 상태를 본문 끝의 trailer 프레임으로 옮긴다.
//
// 2번이 브라우저가 순수 gRPC 를 쓸 수 없는 이유이자, 이런 프록시가 필요한 이유다.

import (
	"context"
	"encoding/base64"
	"encoding/binary"
	"fmt"
	"io"
	"log"
	"net/http"
	"strings"

	"google.golang.org/protobuf/proto"

	profilev1 "github.com/idean3885/grpc-vs-rest-lab/gateway/gen/profile/v1"
)

const (
	frameData    byte = 0x00
	frameTrailer byte = 0x80

	// 바이너리 모드. 데브툴 Response 탭에 바이너리로 나타난다.
	contentTypeProto = "application/grpc-web+proto"
	// base64 모드. 데브툴에서 base64 문자열로 읽히므로 프로토버프 바이트를 눈으로 확인하기 쉽다.
	contentTypeText = "application/grpc-web-text"
)

// grpcWebHandler 는 grpc-web 요청을 받아 gRPC 스텁으로 위임한다.
type grpcWebHandler struct {
	client profilev1.ProfileServiceClient
}

func (h *grpcWebHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	setGrpcWebCORS(w)
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}

	// base64 모드 여부는 요청 content-type 으로 판단한다.
	isText := strings.Contains(r.Header.Get("Content-Type"), "grpc-web-text")

	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "본문 읽기 실패", http.StatusBadRequest)
		return
	}
	if isText {
		decoded, err := base64.StdEncoding.DecodeString(strings.TrimSpace(string(body)))
		if err != nil {
			http.Error(w, "base64 디코딩 실패", http.StatusBadRequest)
			return
		}
		body = decoded
	}

	payload, err := unframe(body)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	respBytes, grpcErr := h.dispatch(r.URL.Path, payload)

	contentType := contentTypeProto
	if isText {
		contentType = contentTypeText
	}
	w.Header().Set("Content-Type", contentType)
	// 브라우저가 gRPC 상태를 읽을 수 있게 헤더로도 노출한다.
	w.Header().Set("X-Served-By", "grpc-web-proxy")

	var out []byte
	if grpcErr != "" {
		// 데이터 프레임 없이 trailer 프레임만 보낸다.
		out = frame(frameTrailer, []byte(fmt.Sprintf("grpc-status: 2\r\ngrpc-message: %s\r\n", grpcErr)))
	} else {
		out = append(frame(frameData, respBytes), frame(frameTrailer, []byte("grpc-status: 0\r\n"))...)
	}

	if isText {
		encoded := base64.StdEncoding.EncodeToString(out)
		out = []byte(encoded)
	}

	if _, err := w.Write(out); err != nil {
		log.Printf("[grpc-web] 응답 쓰기 실패: %v", err)
	}
}

// dispatch 는 경로에 맞는 rpc 를 호출한다.
//
// 실제 Envoy 는 프레임만 변환해 그대로 흘려보내므로 메서드를 알 필요가 없다. 이 구현은
// 스텁을 거치기 때문에 메서드별 분기가 필요하다. 그 대신 요청·응답 타입이 코드에 드러난다.
func (h *grpcWebHandler) dispatch(path string, payload []byte) ([]byte, string) {
	switch path {
	case "/profile.v1.ProfileService/ListProfiles":
		var req profilev1.ListProfilesRequest
		if err := proto.Unmarshal(payload, &req); err != nil {
			return nil, "요청 역직렬화 실패: " + err.Error()
		}
		resp, err := h.client.ListProfiles(context.Background(), &req)
		if err != nil {
			return nil, err.Error()
		}
		out, err := proto.Marshal(resp)
		if err != nil {
			return nil, err.Error()
		}
		return out, ""

	case "/profile.v1.ProfileService/GetProfile":
		var req profilev1.GetProfileRequest
		if err := proto.Unmarshal(payload, &req); err != nil {
			return nil, "요청 역직렬화 실패: " + err.Error()
		}
		resp, err := h.client.GetProfile(context.Background(), &req)
		if err != nil {
			return nil, err.Error()
		}
		out, err := proto.Marshal(resp)
		if err != nil {
			return nil, err.Error()
		}
		return out, ""

	default:
		return nil, "지원하지 않는 메서드: " + path
	}
}

// frame 은 payload 를 길이 접두 프레임으로 감싼다.
func frame(flag byte, payload []byte) []byte {
	out := make([]byte, 5+len(payload))
	out[0] = flag
	binary.BigEndian.PutUint32(out[1:5], uint32(len(payload)))
	copy(out[5:], payload)
	return out
}

// unframe 은 첫 데이터 프레임의 payload 를 꺼낸다.
func unframe(body []byte) ([]byte, error) {
	if len(body) < 5 {
		return nil, fmt.Errorf("프레임이 5바이트보다 짧다: %d", len(body))
	}
	length := binary.BigEndian.Uint32(body[1:5])
	if uint32(len(body)-5) < length {
		return nil, fmt.Errorf("프레임 길이 불일치: 선언=%d, 실제=%d", length, len(body)-5)
	}
	return body[5 : 5+length], nil
}

func setGrpcWebCORS(w http.ResponseWriter) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "content-type,x-grpc-web,grpc-timeout,x-user-agent")
	w.Header().Set("Access-Control-Expose-Headers", "grpc-status,grpc-message,x-served-by,content-length")

	// grpc-web 요청은 항상 프리플라이트를 유발한다. Content-Type(application/grpc-web+proto)이
	// 단순 요청 허용 목록 밖이고 X-Grpc-Web 이 커스텀 헤더이기 때문이다.
	//
	// 이 값을 지정하지 않으면 브라우저 기본값(크롬 5초)으로 캐시되어 OPTIONS 왕복이 반복된다.
	// 크로스 오리진 구성에서 grpc-web 을 쓸 때 실측 지연에 프리플라이트가 섞이는 원인이고,
	// 프로토버프의 크기 이득과 무관한 비용이다. Envoy 의 grpc_web 예제 설정도 같은 값을 쓴다.
	w.Header().Set("Access-Control-Max-Age", "1728000")
}
