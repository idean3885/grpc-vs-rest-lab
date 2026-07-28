#!/usr/bin/env bash
# 랩 전체 종료. 포트 기준으로 리스너만 내린다.
set -uo pipefail
cd "$(dirname "$0")"

stop_port() {
  local port=$1 name=$2
  local pid
  pid=$(lsof -ti:"$port" -sTCP:LISTEN 2>/dev/null)
  if [ -n "$pid" ]; then
    kill "$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null
    echo "  ✓ $name (:$port) 종료"
  else
    echo "  · $name (:$port) 이미 종료"
  fi
}

echo "▶ 종료"
stop_port 8000 "관측 페이지"
stop_port 8090 "gateway"
stop_port 8080 "identity-server"
stop_port 8081 "profile-server"

if docker ps --filter name=grpclab-envoy --format '{{.Names}}' 2>/dev/null | grep -q grpclab-envoy; then
  docker stop grpclab-envoy >/dev/null && echo "  ✓ Envoy 컨테이너 정지"
else
  echo "  · Envoy 이미 정지"
fi

echo "완료. 컨테이너까지 지우려면: docker rm -f grpclab-envoy"
