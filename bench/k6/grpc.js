import grpc from 'k6/net/grpc';
import { check } from 'k6';

const SIZE = __ENV.SIZE || '1';
const ADDR = __ENV.ADDR || 'localhost:9090';

export const options = {
  vus: Number(__ENV.VUS || 50),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    checks: ['rate>0.99'],
  },
};

// 클라이언트를 모듈 스코프에 둔다. VU 마다 인스턴스가 하나씩 생긴다.
const client = new grpc.Client();

export default function () {
  // 첫 반복에서만 연결한다. 매 반복 연결하면 gRPC 쪽에만 커넥션 설정 비용이
  // 반복 계상되어 REST 커넥션 풀과 조건이 어긋난다. k6 로 측정한 gRPC 성능이
  // 나쁘게 나오는 사례의 흔한 원인이다.
  if (__ITER === 0) {
    client.connect(ADDR, { plaintext: true, reflect: true });
  }

  const res = client.invoke(
    'profile.v1.ProfileService/ListProfiles',
    { size: Number(SIZE) },
    { tags: { transport: 'grpc', size: SIZE } },
  );

  check(res, {
    'status OK': (r) => r.status === grpc.StatusOK,
    'body parsed': (r) => r.message.total === Number(SIZE),
  });
}
