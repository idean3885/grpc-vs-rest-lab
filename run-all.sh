#!/usr/bin/env bash
# 랩 전체 기동. 브라우저 관측과 벤치마크에 필요한 프로세스를 모두 띄운다.
#
#   ./run-all.sh          기동 (이미 떠 있으면 건너뛴다)
#   ./run-all.sh --build  빌드부터 다시
#
# 로그는 .run/ 아래에 남는다. 종료는 ./stop-all.sh
set -uo pipefail
cd "$(dirname "$0")"

RUN_DIR=.run
mkdir -p "$RUN_DIR"

listening() { lsof -ti:"$1" -sTCP:LISTEN >/dev/null 2>&1; }

# 포트가 열릴 때까지 기다린다. sleep 대신 curl 재시도를 쓴다.
wait_http() {
  local url=$1 name=$2
  if curl -s --retry 90 --retry-delay 1 --retry-connrefused --max-time 150 -o /dev/null "$url"; then
    echo "  ✓ $name"
  else
    echo "  ✗ $name — 로그 확인: $RUN_DIR/" >&2
    return 1
  fi
}

if [ "${1:-}" = "--build" ]; then
  echo "▶ Java 빌드"
  ./gradlew build -x test --console=plain -q || exit 1
  echo "▶ Go 게이트웨이 빌드"
  (cd gateway && ./generate.sh >/dev/null && go build -o bin/gateway .) || exit 1
fi

# ── 1. profile-server : REST 8081 + gRPC 9090 ──────────────────────
if listening 8081; then
  echo "  · profile-server 이미 기동"
else
  echo "▶ profile-server (8081 REST, 9090 gRPC)"
  nohup java -jar profile-server/build/libs/profile-server-0.0.1-SNAPSHOT.jar \
    > "$RUN_DIR/profile-server.log" 2>&1 &
  disown
  wait_http http://localhost:8081/actuator/health "profile-server"
fi

# ── 2. identity-server : 8080 ──────────────────────────────────────
if listening 8080; then
  echo "  · identity-server 이미 기동"
else
  echo "▶ identity-server (8080)"
  nohup java -jar identity-server/build/libs/identity-server-0.0.1-SNAPSHOT.jar \
    > "$RUN_DIR/identity-server.log" 2>&1 &
  disown
  wait_http http://localhost:8080/actuator/health "identity-server"
fi

# ── 3. grpc-gateway 8090 + grpc-web 프록시 8091 ────────────────────
if listening 8090; then
  echo "  · gateway 이미 기동"
else
  echo "▶ gateway (8090 JSON 트랜스코딩, 8091 grpc-web)"
  nohup gateway/bin/gateway -grpc-addr localhost:9090 -listen :8090 -web-listen :8091 \
    > "$RUN_DIR/gateway.log" 2>&1 &
  disown
  wait_http "http://localhost:8090/v1/profiles?size=1" "gateway"
fi

# ── 4. Envoy grpc-web 8092 (선택) ──────────────────────────────────
if docker ps --filter name=grpclab-envoy --format '{{.Names}}' 2>/dev/null | grep -q grpclab-envoy; then
  echo "  · Envoy 이미 기동 (8092)"
elif docker ps -a --filter name=grpclab-envoy --format '{{.Names}}' 2>/dev/null | grep -q grpclab-envoy; then
  echo "▶ Envoy 재시작 (8092)"
  docker start grpclab-envoy >/dev/null && echo "  ✓ Envoy"
else
  echo "▶ Envoy 생성 (8092)"
  docker run -d --name grpclab-envoy -p 8092:8091 -p 9901:9901 \
    -v "$PWD/envoy/envoy.yaml:/etc/envoy/envoy.yaml:ro" \
    envoyproxy/envoy:v1.34-latest -c /etc/envoy/envoy.yaml --log-level warn >/dev/null 2>&1 &&
    echo "  ✓ Envoy" || echo "  ✗ Envoy (Docker 데몬 확인). 이 경로는 선택이므로 계속 진행" >&2
fi

# ── 5. 브라우저 관측 페이지 8000 ───────────────────────────────────
if listening 8000; then
  echo "  · 관측 페이지 이미 기동"
else
  echo "▶ 관측 페이지 (8000)"
  nohup python3 -m http.server 8000 --bind 127.0.0.1 -d web \
    > "$RUN_DIR/web.log" 2>&1 &
  disown
  wait_http http://localhost:8000/ "관측 페이지"
fi

cat <<'MSG'

기동 완료.

  브라우저 관측   http://localhost:8000
                  Cmd+Option+I 로 데브툴을 열고 Network 탭에서 "전부 호출"

  서비스 간 호출  http://localhost:8080/api/v1/me/rest?size=100
                  http://localhost:8080/api/v1/me/grpc?size=100

  벤치마크        cd bench && ./run.sh layer2

  종료            ./stop-all.sh
MSG
