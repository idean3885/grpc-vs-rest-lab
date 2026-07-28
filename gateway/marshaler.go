package main

import "google.golang.org/protobuf/encoding/protojson"

// protojsonMarshalOptions 는 게이트웨이 JSON 출력 형식을 REST 서버 응답과 맞춘다.
//
//   - EmitUnpopulated: proto3 기본값 필드도 내보낸다. Jackson 이 만드는 REST 응답에는
//     모든 필드가 들어 있으므로, 이걸 끄면 필드 개수가 달라져 크기 비교가 성립하지 않는다.
//   - UseProtoNames=false: userId 처럼 lowerCamelCase 로 낸다. proto 필드명은
//     user_id 지만 REST 서버는 camelCase 를 쓴다.
func protojsonMarshalOptions() protojson.MarshalOptions {
	return protojson.MarshalOptions{
		EmitUnpopulated: true,
		UseProtoNames:   false,
	}
}
