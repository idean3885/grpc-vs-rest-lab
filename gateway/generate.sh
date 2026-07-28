#!/usr/bin/env bash
# .proto 에서 Go 스텁과 grpc-gateway 리버스 프록시를 생성한다.
#
# google/api/annotations.proto 는 Java 빌드가 추출해 둔 경로를 그대로 쓴다.
# 없으면 먼저 ../gradlew :proto:build 를 실행한다.
set -euo pipefail

cd "$(dirname "$0")"
ROOT=..
INCLUDE_GOOGLE="$ROOT/proto/build/extracted-include-protos/main"

if [ ! -d "$INCLUDE_GOOGLE/google/api" ]; then
  echo "google/api proto 가 없다. '$ROOT/gradlew :proto:build' 를 먼저 실행한다." >&2
  exit 1
fi

export PATH="$PATH:$(go env GOPATH)/bin"
mkdir -p gen

protoc \
  -I "$ROOT/proto/src/main/proto" \
  -I "$INCLUDE_GOOGLE" \
  --go_out=gen --go_opt=module=github.com/idean3885/grpc-vs-rest-lab/gateway/gen \
  --go-grpc_out=gen --go-grpc_opt=module=github.com/idean3885/grpc-vs-rest-lab/gateway/gen \
  --grpc-gateway_out=gen --grpc-gateway_opt=module=github.com/idean3885/grpc-vs-rest-lab/gateway/gen \
  profile/v1/profile.proto

echo "생성 완료:"
find gen -name "*.go" | sort
