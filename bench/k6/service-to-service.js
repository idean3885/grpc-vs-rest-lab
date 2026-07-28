import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

// 서비스 간 호출 측정 (층 2).
//
// identity-server 가 RestClient 선언형 인터페이스와 gRPC 스텁으로 각각 profile-server 를
// 호출한다. k6 는 identity-server 의 얇은 요약 응답만 받으므로, k6 측 역직렬화 비용이
// 양쪽에 동일하게 걸린다. 층 1(프로토콜 직접 측정)에서 k6 의 프로토버프 파싱 비용이
// gRPC 쪽에만 얹히던 문제를 이 층에서 제거한다.
//
// TRANSPORT: rest | grpc
const TRANSPORT = __ENV.TRANSPORT || 'rest';
const SIZE = __ENV.SIZE || '1';
const BASE = __ENV.BASE || 'http://localhost:8080';

// identity-server 가 자체 측정한 상류 왕복 시간. k6 관측 지연에서 identity 자신의
// 처리 시간을 뺀 값에 해당한다.
// isTime 을 켜지 않는다. k6 의 시간 서식은 값을 밀리초로 가정하므로, 마이크로초 값을 넣으면
// 1000배 부풀려 표시된다.
const upstream = new Trend('upstream_micros');

export const options = {
  vus: Number(__ENV.VUS || 50),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    checks: ['rate>0.99'],
  },
};

export default function () {
  const res = http.get(`${BASE}/api/v1/me/${TRANSPORT}?size=${SIZE}`, {
    tags: { transport: TRANSPORT, size: SIZE },
  });

  const ok = check(res, {
    'status 200': (r) => r.status === 200,
  });

  if (ok) {
    const body = res.json();
    upstream.add(body.upstreamElapsedMicros, { transport: TRANSPORT, size: SIZE });
    check(body, {
      'count matches': (b) => b.profileCount === Number(SIZE),
    });
  }
}
