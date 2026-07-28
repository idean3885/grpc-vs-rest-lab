import http from 'k6/http';
import { check } from 'k6';

// SIZE: 프로필 건수 (페이로드 크기 축)
// VUS: 동시 사용자 수
// DURATION: 측정 시간
const SIZE = __ENV.SIZE || '1';
const BASE = __ENV.BASE || 'http://localhost:8081';

export const options = {
  vus: Number(__ENV.VUS || 50),
  duration: __ENV.DURATION || '30s',
  // 워밍업은 별도 실행으로 분리한다. 여기서 섞으면 첫 구간의 JIT 미적용 값이 p99 를 왜곡한다.
  thresholds: {
    checks: ['rate>0.99'],
  },
  // 응답 본문을 버리지 않는다. 버리면 역직렬화 비용이 측정에서 빠져 비교가 성립하지 않는다.
  discardResponseBodies: false,
};

export default function () {
  const res = http.get(`${BASE}/v1/profiles?size=${SIZE}`, {
    tags: { transport: 'rest', size: SIZE },
  });

  check(res, {
    'status 200': (r) => r.status === 200,
    'body parsed': (r) => {
      // 본문을 실제로 파싱해 REST 쪽 역직렬화 비용을 측정에 포함시킨다.
      const body = r.json();
      return body.total === Number(SIZE);
    },
  });
}
