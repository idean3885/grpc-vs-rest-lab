// grpc-gateway 리버스 프록시.
//
// 브라우저나 curl 이 보낸 JSON/HTTP1.1 요청을 gRPC 호출로 번역해 profile-server 로 넘긴다.
// 경로 매핑은 코드에 없다. .proto 의 google.api.http 옵션에서 생성된 것이다.
//
// 주목할 점은 이 프록시가 Java 서버와 아무 관계가 없다는 것이다. gRPC 클라이언트일 뿐이므로
// 서버가 어느 언어로 구현됐는지 모른다. grpc-gateway 가 Go 전용 도구인데도 Java 서버 앞에
// 붙을 수 있는 이유다.
package main

import (
	"context"
	"flag"
	"log"
	"net/http"

	"github.com/grpc-ecosystem/grpc-gateway/v2/runtime"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	profilev1 "github.com/idean3885/grpc-vs-rest-lab/gateway/gen/profile/v1"
)

func main() {
	grpcAddr := flag.String("grpc-addr", "localhost:9090", "profile-server gRPC 주소")
	listen := flag.String("listen", ":8090", "게이트웨이(JSON 트랜스코딩) 리스닝 주소")
	webListen := flag.String("web-listen", ":8091", "grpc-web 프록시 리스닝 주소")
	flag.Parse()

	// 채널 하나를 재사용한다. 요청마다 만들면 게이트웨이가 병목이 된다.
	conn, err := grpc.NewClient(
		*grpcAddr,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		log.Fatalf("[gateway] gRPC 연결 실패: %v", err)
	}
	defer conn.Close()

	// EmitUnpopulated 을 켜서 기본값 필드도 JSON 에 내보낸다. 끄면 proto3 기본값
	// (0, false, 빈 문자열)이 응답에서 빠져 REST 서버 응답과 필드 구성이 달라지고,
	// 그 상태로 페이로드 크기를 비교하면 게이트웨이 쪽이 부당하게 작게 나온다.
	mux := runtime.NewServeMux(
		runtime.WithMarshalerOption(runtime.MIMEWildcard, &runtime.JSONPb{
			MarshalOptions: protojsonMarshalOptions(),
		}),
	)

	if err := profilev1.RegisterProfileServiceHandler(context.Background(), mux, conn); err != nil {
		log.Fatalf("[gateway] 핸들러 등록 실패: %v", err)
	}

	// grpc-web 프록시를 별 포트에서 함께 띄운다. 같은 gRPC 채널을 공유한다.
	webHandler := &grpcWebHandler{client: profilev1.NewProfileServiceClient(conn)}
	go func() {
		log.Printf("[grpc-web] listening on %s, upstream=%s", *webListen, *grpcAddr)
		if err := http.ListenAndServe(*webListen, webHandler); err != nil {
			log.Fatalf("[grpc-web] 서버 종료: %v", err)
		}
	}()

	log.Printf("[gateway] listening on %s, upstream=%s", *listen, *grpcAddr)
	if err := http.ListenAndServe(*listen, withCORS(mux)); err != nil {
		log.Fatalf("[gateway] 서버 종료: %v", err)
	}
}

// withCORS 는 브라우저 관측 페이지(다른 포트에서 서빙)가 호출할 수 있게 한다.
// 로컬 랩 전용이므로 모든 오리진을 허용한다.
func withCORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Accept")
		// 데브툴에서 게이트웨이 경유임을 구별할 수 있게 표시를 남긴다.
		// 이 헤더가 없으면 REST 서버 응답과 완전히 동일해 구분이 불가능하다.
		w.Header().Set("X-Served-By", "grpc-gateway")
		w.Header().Set("Access-Control-Expose-Headers", "X-Served-By, Content-Length")

		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}
