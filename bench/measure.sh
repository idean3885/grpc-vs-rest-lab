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
# 가상 스레드 축은 서버 기동 옵션으로 갈린다. 라벨을 손으로 붙이지 않고 실행 중인
# profile-server 스레드 덤프에서 판정해 결과 파일명에 반영한다. 잘못 붙은 라벨로
# 두 회차를 섞는 사고를 막는다.
#   VIRTUAL_THREADS=false ../run-all.sh   →  results/rest-size1.*
#   VIRTUAL_THREADS=true  ../run-all.sh   →  results/rest-vt-size1.*
#
# 수집 항목:
#   - TCP 커넥션 수 (실제 커넥션. lsof 는 양쪽 소켓을 각각 세므로 2로 나눈다)
#   - 스레드 수 (그룹별. 가상 스레드 모드면 ForkJoinPool 캐리어가 대상)
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

# 가상 스레드 모드 판정.
#
# Tomcat 이 가상 스레드로 돌면 요청 처리 스레드가 플랫폼 스레드가 아니게 되어
# http-nio-<port>-exec-N 이 스레드 덤프에서 사라진다. Acceptor·Poller 는 두 모드
# 모두 플랫폼 스레드로 남으므로 exec 유무만으로 판정한다.
detect_thread_mode() {
  if jcmd "$PID" Thread.print 2>/dev/null | grep -q "\"http-nio-${REST_PORT}-exec-"; then
    echo platform
  else
    echo virtual
  fi
}

THREAD_MODE=$(detect_thread_mode)

# gRPC 경로는 Netty 이벤트 루프라 이 플래그에 영향받지 않는다. 라벨을 붙이면
# 같은 조건을 두 이름으로 쓰게 되므로 REST 일 때만 구분한다.
LABEL=$TRANSPORT
if [ "$TRANSPORT" != "grpc" ] && [ "$THREAD_MODE" = "virtual" ]; then
  LABEL="${TRANSPORT}-vt"
fi
RESULT="results/${LABEL}-size${SIZE}"

# lsof 는 클라이언트측·서버측 소켓을 각각 세므로 실제 커넥션은 절반이다.
conn_count() {
  local raw
  raw=$(lsof -nP -iTCP:"$1" -sTCP:ESTABLISHED 2>/dev/null | grep -c ":$1" || echo 0)
  echo $((raw / 2))
}

# 플랫폼 스레드 그룹. 가상 스레드는 여기 나오지 않고 캐리어(ForkJoinPool worker)만
# 보인다. 그 캐리어 수가 고정인지 동시성에 비례하는지가 관측 지점이다.
thread_groups() {
  jcmd "$PID" Thread.print 2>/dev/null |
    grep -oE '^"[^"]+' | sed 's/"//' | sed 's/-[0-9]*$//' |
    sort | uniq -c | sort -rn | grep -E "http-nio|grpc-|ForkJoinPool" | head -6
}

# 프로세스 누적 CPU 시간(초). macOS ps 는 MM:SS.ss 또는 HH:MM:SS.ss 형태로 낸다.
cpu_seconds() {
  ps -o time= -p "$PID" 2>/dev/null | tr -d ' ' |
    awk -F: '{ if (NF==3) printf "%.2f", $1*3600+$2*60+$3; else if (NF==2) printf "%.2f", $1*60+$2; else printf "0" }'
}

echo "▶ $TRANSPORT / size=$SIZE / ${VUS}VU / $DURATION"
echo "  대상 포트 :$TARGET_PORT, profile-server PID=$PID"
if [ "$TRANSPORT" = "grpc" ]; then
  echo "  상류 스레드: $THREAD_MODE (gRPC 경로는 Netty 라 이 값과 무관)"
else
  echo "  상류 스레드: $THREAD_MODE"
fi
echo

# 워밍업. JIT 미적용 구간을 측정에서 제외한다.
echo "  워밍업 ($WARMUP)..."
TRANSPORT=$TRANSPORT SIZE=$SIZE VUS=10 DURATION=$WARMUP \
  k6 run --quiet --summary-mode=disabled k6/service-to-service.js >/dev/null 2>&1

CPU_BEFORE=$(cpu_seconds)

# 본 측정
TRANSPORT=$TRANSPORT SIZE=$SIZE VUS=$VUS DURATION=$DURATION \
  k6 run --summary-export="${RESULT}.json" \
  k6/service-to-service.js > "${RESULT}.log" 2>&1 &
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
  "${RESULT}.log" | sed 's/^/    /'
echo
echo "  상세 로그 : ${RESULT}.log"
