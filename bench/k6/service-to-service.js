import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

// 경유 측정: 애플리케이션을 거쳐 서비스 간 호출을 잰다.
//
// identity-server 가 RestClient 선언형 인터페이스와 gRPC 스텁으로 각각 profile-server 를
// 호출한다. k6 는 identity-server 의 얇은 요약 응답만 받으므로, k6 측 역직렬화 비용이
// 양쪽에 동일하게 걸린다. 직접 측정에서 k6 의 프로토버프 파싱 비용이 gRPC 쪽에만
// 얹히던 문제를 이렇게 제거한다.
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

// 실행 모델을 두 가지로 나눈다.
//
// RATE 없음 (닫힌 루프): VU 수를 고정하고 응답이 오는 대로 다음 요청을 던진다. 처리량
//   상한을 찾는 데 맞지만, 서버가 느려지면 부하도 같이 줄어들어 과부하 구간의 지연을
//   보지 못한다. 정본 3축 회차가 이 모델이다.
//
// RATE 지정 (열린 루프): 요청률을 고정하고 응답 여부와 무관하게 계속 도착시킨다. 서버가
//   따라오지 못하면 지연과 실패로 나타나므로 "무엇이 먼저 마르는가" 를 볼 수 있다.
//   커넥션 예산을 낮춰 재는 회차에 쓴다.
const RATE = Number(__ENV.RATE || 0);

export const options =
  RATE > 0
    ? {
        scenarios: {
          openLoop: {
            executor: 'constant-arrival-rate',
            rate: RATE,
            timeUnit: '1s',
            duration: __ENV.DURATION || '15s',
            preAllocatedVUs: Number(__ENV.PRE_VUS || 50),
            // 재사용 가능한 VU 상한. 응답이 밀려 VU 가 모두 점유되면 k6 가 요청을
            // 버리고 dropped_iterations 로 센다. 그 값이 과부하 지표가 된다.
            maxVUs: Number(__ENV.MAX_VUS || 400),
          },
        },
        // 과부하를 만드는 것이 목적이라 임계값을 걸지 않는다. 실패율 자체가 관측 대상이다.
      }
    : {
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
