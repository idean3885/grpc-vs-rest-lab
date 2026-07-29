#!/usr/bin/env bash
# 커넥션 예산 회차 결과를 한 표로 모은다.
#
# dropped_iterations 는 --summary-export JSON 에 담기지 않으므로 로그에서 뽑는다.
# 이 값이 0 보다 크면 목표 요청률을 채우지 못한 것이고, 그 지점이 포화 신호다.
set -uo pipefail
cd "$(dirname "$0")"

printf '%-6s %-6s %-6s %10s %10s %10s %10s %10s %8s\n' \
  전송 요청률 풀 달성률 상류p50 상류p95 상류max 관측p95 버림
printf '%.0s-' {1..90}; echo

for f in $(ls results/conn-*.json 2>/dev/null | sort -t- -k4 -V); do
  base=${f%.json}
  name=$(basename "$base")
  transport=$(echo "$name" | sed -E 's/^conn-([a-z-]+)-size.*/\1/')
  rate=$(echo "$name" | sed -E 's/.*-rate([0-9]+).*/\1/')
  pool=$(echo "$name" | sed -E 's/.*-pool([0-9]+).*/\1/')

  read -r achieved up50 up95 upmax obs95 <<<"$(jq -r '
    [ (.metrics.iterations.rate      // 0 | . * 10 | round / 10),
      (.metrics.upstream_micros.med  // 0 | . | round),
      (.metrics.upstream_micros["p(95)"] // 0 | . | round),
      (.metrics.upstream_micros.max  // 0 | . | round),
      (.metrics.http_req_duration["p(95)"] // 0 | . * 100 | round / 100)
    ] | @tsv' "$f")"

  dropped=$(grep -oE 'dropped_iterations[^0-9]*([0-9]+)' "$base.log" 2>/dev/null |
    grep -oE '[0-9]+$' | head -1)
  dropped=${dropped:-0}

  printf '%-6s %-6s %-6s %10s %8sµs %8sµs %8sµs %8sms %8s\n' \
    "$transport" "$rate" "$pool" "$achieved" "$up50" "$up95" "$upmax" "$obs95" "$dropped"
done
