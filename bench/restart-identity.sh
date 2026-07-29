#!/usr/bin/env bash
# identity-server 를 지정한 REST 커넥션 풀 크기로 다시 띄운다.
#
#   ./restart-identity.sh 1     커넥션 예산 회차 조건
#   ./restart-identity.sh 200   정본 3축 회차 조건으로 복구
#
# 풀 크기만 갈리고 나머지는 application.yml 그대로다. 톰캣 워커 상한만 올려 둔다.
# 풀이 마를 때 워커까지 같이 마르면 두 병목이 겹쳐 축을 가릴 수 없기 때문이다.
# 커넥션이 유일한 희소 자원이어야 한다.
set -uo pipefail
cd "$(dirname "$0")/.."

POOL=${1:-200}
RUN_DIR=.run
JAR=identity-server/build/libs/identity-server-0.0.1-SNAPSHOT.jar

# Java 21 런타임을 명시적으로 고른다. PATH 의 java 가 그보다 낮으면 jar 가 뜨지 않고
# UnsupportedClassVersionError 로 죽는다. JDK 를 여러 개 깔아 둔 환경에서 재현이 깨지는 지점이다.
JAVA_BIN=java
if [ -x /usr/libexec/java_home ]; then
  JAVA_21_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
  [ -n "$JAVA_21_HOME" ] && JAVA_BIN="$JAVA_21_HOME/bin/java"
fi

mkdir -p "$RUN_DIR"

OLD_PID=$(lsof -ti:8080 -sTCP:LISTEN 2>/dev/null | head -1)
if [ -n "$OLD_PID" ]; then
  echo "▶ 기존 identity-server 종료 (PID $OLD_PID)"
  kill "$OLD_PID" 2>/dev/null
  # 포트가 풀릴 때까지 기다린다. 재바인딩 실패로 조용히 옛 프로세스가 남는 것을 막는다.
  for _ in $(seq 1 60); do
    lsof -ti:8080 -sTCP:LISTEN >/dev/null 2>&1 || break
    curl -s -o /dev/null --max-time 1 http://localhost:8081/actuator/health 2>/dev/null
  done
fi

echo "▶ identity-server 기동 · REST 커넥션 풀=${POOL}"
PROFILE_CLIENT_MAXCONNECTIONS="$POOL" \
  SERVER_TOMCAT_THREADS_MAX=500 \
  nohup "$JAVA_BIN" -jar "$JAR" > "$RUN_DIR/identity-server.log" 2>&1 &
disown

if curl -s --retry 90 --retry-delay 1 --retry-connrefused --max-time 150 \
  -o /dev/null http://localhost:8080/actuator/health; then
  echo "  ✓ identity-server (풀 ${POOL})"
else
  echo "  ✗ identity-server (로그: $RUN_DIR/identity-server.log)" >&2
  exit 1
fi
