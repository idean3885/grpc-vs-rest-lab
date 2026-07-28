#!/usr/bin/env bash
# 벤치마크 실행. 워밍업을 본 측정과 분리한다.
#
# 사용법:
#   ./run.sh layer1          프로토콜 직접 측정 (REST 8081 vs gRPC 9090)
#   ./run.sh layer2          서비스 간 호출 측정 (identity-server 8080 경유)
#   ./run.sh all
set -euo pipefail

cd "$(dirname "$0")"
RESULTS=results
mkdir -p "$RESULTS"

SIZES=${SIZES:-"1 100 1000 10000"}
VUS=${VUS:-50}
DURATION=${DURATION:-20s}
WARMUP_DURATION=${WARMUP_DURATION:-10s}

run_one() {
  local label=$1 script=$2
  shift 2
  echo "▶ $label"
  # 워밍업. JIT 미적용 구간을 집계에서 제외하기 위해 별도 실행한다.
  env "$@" VUS=10 DURATION="$WARMUP_DURATION" \
    k6 run --quiet --summary-mode=disabled "$script" >/dev/null 2>&1 || true
  # 본 측정
  env "$@" VUS="$VUS" DURATION="$DURATION" \
    k6 run --summary-export="$RESULTS/${label}.json" "$script" 2>&1 |
    grep -E "iterations\.|_req_duration|data_received|checks_succeeded" || true
  echo
}

layer1() {
  for size in $SIZES; do
    run_one "layer1-rest-size${size}" k6/rest.js "SIZE=$size"
    run_one "layer1-grpc-size${size}" k6/grpc.js "SIZE=$size"
  done
}

layer2() {
  for size in $SIZES; do
    run_one "layer2-rest-size${size}" k6/service-to-service.js "TRANSPORT=rest" "SIZE=$size"
    run_one "layer2-grpc-size${size}" k6/service-to-service.js "TRANSPORT=grpc" "SIZE=$size"
  done
}

case "${1:-all}" in
  layer1) layer1 ;;
  layer2) layer2 ;;
  all) layer1; layer2 ;;
  *) echo "usage: $0 [layer1|layer2|all]" >&2; exit 1 ;;
esac
