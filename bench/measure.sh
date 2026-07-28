#!/usr/bin/env bash
# 부하 중 서버 자원 지표를 수집한다.
#
# k6 는 클라이언트 관점 지표(처리량·지연)만 낸다. 프로토콜을 비교할 때는 서버 쪽
# 자원 지표가 함께 있어야 해석이 된다. 특히 커넥션 수는 대규모에서 지연보다 먼저
# 병목이 되는 축인데 k6 로는 보이지 않는다.
#
# 사용법:
#   ./measure.sh rest 1        전송방식 건수
#   ./measure.sh grpc 1000
#
# 수집 항목:
#   - TCP 커넥션 수 (실제 커넥션. lsof 는 양쪽 소켓을 각각 세므로 2로 나눈다)
#   - 스레드 수 (그룹별)
#   - CPU 시간 증가분 (부하 구간에서 소비한 CPU 초)
set -uo pipefail
cd "$(dirname "$0")"

TRANSPORT=${1:-rest}
SIZE=${2:-1}
VUS=${VUS:-50}
DURATION=${DURATION:-30s}
WARMUP=${WARMUP:-10s}

REST_PORT=8081
GRPC_PORT=9090
[ "$TRANSPORT" = "grpc" ] && TARGET_PORT=$GRPC_PORT || TARGET_PORT=$REST_PORT

PID=$(lsof -ti:$REST_PORT -sTCP:LISTEN 2>/dev/null | head -1)
if [ -z "$PID" ]; then
  echo "profile-server 가 떠 있지 않다. ../run-all.sh 를 먼저 실행한다." >&2
  exit 1
fi

# lsof 는 클라이언트측·서버측 소켓을 각각 세므로 실제 커넥션은 절반이다.
conn_count() {
  local raw
  raw=$(lsof -nP -iTCP:"$1" -sTCP:ESTABLISHED 2>/dev/null | grep -c ":$1" || echo 0)
  echo $((raw / 2))
}

thread_groups() {
  jcmd "$PID" Thread.print 2>/dev/null |
    grep -oE '^"[^"]+' | sed 's/"//' | sed 's/-[0-9]*$//' |
    sort | uniq -c | sort -rn | grep -E "http-nio|grpc-" | head -5
}

# 프로세스 누적 CPU 시간(초). macOS ps 는 MM:SS.ss 또는 HH:MM:SS.ss 형태로 낸다.
cpu_seconds() {
  ps -o time= -p "$PID" 2>/dev/null | tr -d ' ' |
    awk -F: '{ if (NF==3) printf "%.2f", $1*3600+$2*60+$3; else if (NF==2) printf "%.2f", $1*60+$2; else printf "0" }'
}

echo "▶ $TRANSPORT / size=$SIZE / ${VUS}VU / $DURATION"
echo "  대상 포트 :$TARGET_PORT, profile-server PID=$PID"
echo

# 워밍업. JIT 미적용 구간을 측정에서 제외한다.
echo "  워밍업 ($WARMUP)..."
TRANSPORT=$TRANSPORT SIZE=$SIZE VUS=10 DURATION=$WARMUP \
  k6 run --quiet --summary-mode=disabled k6/service-to-service.js >/dev/null 2>&1

CPU_BEFORE=$(cpu_seconds)

# 본 측정
TRANSPORT=$TRANSPORT SIZE=$SIZE VUS=$VUS DURATION=$DURATION \
  k6 run --summary-export="results/${TRANSPORT}-size${SIZE}.json" \
  k6/service-to-service.js > "results/${TRANSPORT}-size${SIZE}.log" 2>&1 &
K6_PID=$!

# 부하가 정상 상태에 이른 뒤 샘플링. sleep 대신 헬스체크 반복으로 시간을 보낸다.
for _ in $(seq 1 25); do curl -s -o /dev/null --max-time 1 http://localhost:8080/actuator/health; done

echo "  [부하 중 스냅샷]"
echo "    TCP 커넥션 : $(conn_count $TARGET_PORT) 개"
echo "    스레드 그룹 :"
thread_groups | sed 's/^/      /'

wait $K6_PID
CPU_AFTER=$(cpu_seconds)

echo
echo "  [결과]"
echo "    CPU 소비  : $(awk -v a="$CPU_AFTER" -v b="$CPU_BEFORE" 'BEGIN{printf "%.2f", a-b}') 초 (부하 구간)"
grep -E "iterations\.|http_req_duration|upstream_micros|checks_succeeded" \
  "results/${TRANSPORT}-size${SIZE}.log" | sed 's/^/    /'
echo
echo "  상세 로그 : results/${TRANSPORT}-size${SIZE}.log"
