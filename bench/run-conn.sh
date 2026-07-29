#!/usr/bin/env bash
# 커넥션 예산 회차.
#
# 정본 3축 회차가 답하지 못한 질문 하나만 본다.
#   "커넥션이 마르면 지연이 어느 쪽으로 갈리는가."
#
# 정본 회차는 닫힌 루프(VU 50 고정)에 커넥션 풀 200이었다. 풀이 병목에 한참 못 미쳐서
# gRPC 의 다중화 이점이 나타날 조건이 아니었다. 부하를 올려 풀 한계에 닿게 하려면
# 27,000 req/s 대를 다시 만들어야 하고, 그러면 커넥션 고갈·CPU 포화·k6 경합 세 가지가
# 섞여서 무엇 때문에 느려졌는지 가릴 수 없다.
#
# 그래서 반대로 간다. 부하를 올리지 않고 커넥션 예산을 1개로 낮춘다.
#   REST/1.1 : 커넥션 1개는 한 번에 요청 1건 (파이프라이닝은 실무에서 쓰지 않는다)
#   gRPC/2   : 커넥션 1개에 스트림 여러 개를 다중화
# 같은 커넥션 1개로 각 프로토콜이 얼마를 나르는지를 재는 형태가 된다.
#
# 부하는 최대 1,600 req/s 로 정본 회차의 약 6% 다. 측정 중에도 노트북을 쓸 수 있다.
#
# 얻는 것: 메커니즘과 방향.
# 얻지 못하는 것: 프로덕션 용량 수치. 풀을 인위적으로 낮췄으므로 "REST 는 몇 req/s 에서
#   무너진다" 는 말은 할 수 없다. 정본 3축 표와 같은 줄에 섞지 않는다.
#
# 사전 조건: identity-server 를 낮춘 풀로 다시 띄운다.
#   ./bench/restart-identity.sh 1     커넥션 풀 1개
#   ./bench/restart-identity.sh 200   정본 조건으로 복구
set -uo pipefail
cd "$(dirname "$0")"

POOL=${POOL:-1}
RATES=${RATES:-"400 800 1600"}
DURATION=${DURATION:-15s}
TRANSPORTS=${TRANSPORTS:-"rest grpc"}

echo "커넥션 예산 회차 · 풀=${POOL} · 요청률 ${RATES} · ${DURATION}"
echo

for rate in $RATES; do
  for transport in $TRANSPORTS; do
    RATE=$rate POOL=$POOL DURATION=$DURATION MAX_VUS=400 PRE_VUS=50 \
      ./measure.sh "$transport" 1
  done
done
