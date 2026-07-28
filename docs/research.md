# 리서치

측정 전에 확인한 사실과 근거.

## 1. gRPC 연혁과 Spring 의 지원 시점

### 연혁

| 시점 | 사건 | 출처 |
|------|------|------|
| 2015 | Google 이 gRPC 공개. 사내 RPC 프레임워크 Stubby 를 범용화한 것 | [gRPC: Up & Running](https://grpc.io) |
| 2016 | 커뮤니티 스타터 `net.devh:grpc-spring-boot-starter` 등장 | [yidongnan/grpc-spring-boot-starter](https://github.com/yidongnan/grpc-spring-boot-starter) |
| 2022 | Kubernetes 1.24 에서 gRPC health probe beta (기본 활성), 1.27 GA | [Kubernetes 문서](https://kubernetes.io) |
| 2024-04 | 커뮤니티 스타터 마지막 릴리스 3.1.0 (Spring Boot 3.2.4 기준) | [grpc-ecosystem/grpc-spring releases](https://github.com/grpc-ecosystem/grpc-spring/releases) |
| 2024 | Kubernetes Gateway API `GRPCRoute` stable (v1.1) | [Gateway API](https://gateway-api.sigs.k8s.io/reference/api-types/grpcroute/) |
| 2025-05-13 | Spring gRPC, 실험 프로젝트에서 Spring 포트폴리오 정식 승격 | [Spring gRPC Promoted!](https://spring.io/blog/2025/05/13/spring-grpc-promoted/) |
| 2025-11-05 | Boot 4.0 병합 실패 공지. 4.1 로 연기 | [Spring gRPC Next Steps for 1.0.0](https://spring.io/blog/2025/11/05/spring-grpc-next-steps/) |
| 2025-12-04 | Spring gRPC 1.0.0 GA. Boot 4 · Framework 7 지원, Java 17 baseline 유지 | [Spring gRPC 1.0.0 goes GA](https://spring.io/blog/2025/12/04/spring-grpc-1/) |
| Boot 4.1 (예정) | 자동설정이 Spring Boot 코어로 병합 | 위 Next Steps 문서 |

### "왜 이제야 정식 지원인가"

세 가지 오해를 걸러야 한다.

**오해 1. gRPC 가 최신 기술이라서.** gRPC 는 2015 년 공개다. grpc-gateway 도 비슷한 시기에 나왔고 v2 가 2020 년이다. 둘 다 10 년 된 기술이다.

**오해 2. Kubernetes 생태계도 최근에 붙어서.** Kubernetes 는 프로젝트 내부 인터페이스 다수를 gRPC 로 구현하고 있고, health probe 는 2022 년에, Gateway API 의 `GRPCRoute` 는 2024 년에 정식화됐다. 인프라 계층은 이미 gRPC 를 1급으로 다뤄 왔다.

**오해 3. Boot 4 가 새로워서 4.1 로 밀린 것.** Spring 팀은 자동설정을 Boot **4.0** 에 넣으려 했다. 공식 공지의 표현은 이렇다.

> "The original plan was to move the autoconfiguration from Spring gRPC into Spring Boot in time for the 4.0 release. Unfortunately we haven't been able to find the time to merge that change (...) most likely in an early milestone of Spring Boot 4.1."

즉 기술적 장벽이 아니라 **일정과 리소스** 문제였다. 그리고 그 앞단에는 더 긴 지연이 있다. Spring 팀이 공식 프로젝트를 시작한 것 자체가 2024 년이다. 그 전 8 년은 커뮤니티 스타터가 그 자리를 채웠고, 동작했기 때문에 우선순위가 밀렸다고 읽는 것이 자연스럽다.

정리하면 **늦은 쪽은 Spring** 이다. gRPC 도 grpc-gateway 도 Kubernetes 도 아니다.

### 실무에 미치는 영향

Boot 버전별로 선택지가 갈린다.

| Boot 버전 | 선택지 | 상태 |
|-----------|--------|------|
| 2.x | `net.devh` 2.15.x | 유지 중단 |
| 3.0 ~ 3.4 | `net.devh` 3.x (3.2.4 기준 빌드) | 유지 중단, 상위 버전 동작은 비보증 |
| 3.5 | 공식 스타터 없음 | **공백 구간** |
| 4.0 | `org.springframework.grpc:spring-grpc-spring-boot-starter` 1.0.x | GA |
| 4.1+ | Spring Boot 코어 자동설정 | 예정 |

Boot 3.5 는 3.x 마지막 라인이고 OSS 지원이 2032-06-30 까지다. 즉 **앞으로 몇 년간 Boot 3.5 에 머무는 조직이 적지 않을 텐데, 그 구간에 공식 스타터가 없다.**

### 그래서 스타터를 쓰지 않는다

세 후보 중 Boot 3.5 에서 성립하는 것이 없다. 공식 스타터는 Boot 4.1 라인을 요구하고, 커뮤니티 스타터는 Boot 3.2.4 기준으로 빌드된 뒤 유지가 끊겼다. 검증되지 않은 의존성이 끼면 측정값 이상이 스타터 문제인지 프로토콜 특성인지 판단할 수 없다.

그래서 grpc-java 를 빈으로 직접 등록한다. `ServerBuilder.forPort(...).addService(...).build().start()` 를 빈으로 만들고 `destroyMethod` 를 지정하면 생명주기가 컨테이너에 붙는다. 코드 30 줄 정도다 (`profile/adapter/.../grpc/configs/GrpcServerConfig.java`).

얻는 것과 잃는 것은 이렇다.

| | 내용 |
|---|---|
| 얻음 | 서버가 어느 포트에서 어느 스레드 풀로 뜨고 언제 종료되는지가 코드에 드러난다. 수치를 해석할 때 숨은 자동설정을 의심할 필요가 없다 |
| 얻음 | Boot 4.1 로 올릴 때 이 설정 클래스만 지우면 된다. 서비스 구현은 `ProfileServiceGrpc.ProfileServiceImplBase` 를 상속한 표준 형태라 그대로다 |
| 잃음 | 보안 연동·메트릭 자동 등록·인터셉터 자동 스캔이 없다. 프로덕션에서 필요하면 Boot 4.1 이후로 올려 공식 스타터를 쓰는 편이 낫다 |

## 2. 브라우저는 순수 gRPC 를 쓸 수 없다

### 이유

gRPC 는 호출 결과 상태를 응답 본문이 아니라 **HTTP/2 trailer**(본문 뒤에 오는 헤더 블록)에 담는다. `grpc-status`, `grpc-message` 가 거기 실린다.

브라우저의 `fetch` 와 `XMLHttpRequest` 는 trailer 를 읽는 API 를 노출하지 않는다. 또한 HTTP/2 프레임을 직접 제어할 수도 없다. 그래서 브라우저에서 gRPC 서버를 부르려면 중간 계층이 필요하다.

### 선택지 비교

| 방식 | 브라우저 to 프록시 | 프록시 to 서버 | 데브툴에서 보이는 것 |
|------|------------------|---------------|---------------------|
| REST 서버 직접 | HTTP/1.1 + JSON | 없음 | JSON |
| grpc-gateway | HTTP/1.1 + JSON | HTTP/2 + protobuf | JSON (REST 와 동일) |
| grpc-web + Envoy | HTTP/1.1 + protobuf 프레임 | HTTP/2 + protobuf | 바이너리 |

**grpc-gateway 를 도입하면 브라우저 쪽은 그냥 REST 다.** 서버가 gRPC 라는 사실이 클라이언트에게 보이지 않고, 데브툴에도 프로토버프가 나타나지 않는다. 이 사실이 "gRPC 를 쓰면 데브툴에서 프로토버프가 보인다"는 흔한 기대와 어긋난다.

프로토버프가 실제로 어떤 바이트로 오가는지 눈으로 보려면 grpc-web 경로가 필요하다. Envoy 의 `grpc_web` 필터가 브라우저의 base64/바이너리 프레임을 gRPC 프레임으로 변환한다.

Envoy 를 쓰는 것은 스택상으로도 자연스럽다. Istio 의 데이터플레인이 Envoy 이므로, 서비스 메시를 쓰는 환경에서는 이미 경로에 들어 있는 컴포넌트다.

관측 결과는 [browser-observation.md](browser-observation.md).

## 3. 인용되는 성능 수치가 서로 다른 이유

공개 자료의 수치 범위가 넓다. "REST 대비 7~10배", "작은 페이로드에서 최대 77% 낮은 지연, 큰 페이로드에서는 15%", "직렬화 크기 10배 감소" 같은 값이 함께 돌아다니고, 반대로 벤치마크에서 gRPC 가 더 느리게 나왔다는 보고도 있다.

차이가 나는 지점은 대개 측정 조건이다.

- **커넥션 재사용 여부.** gRPC 채널은 커넥션을 유지하는데 REST 쪽 keep-alive 를 끄면 REST 에 핸드셰이크 비용이 반복 계상된다. 반대로 부하 도구에서 가상 사용자마다 gRPC 채널을 새로 만들면 gRPC 가 불리해진다.
- **워밍업.** JVM 에서 첫 호출은 클래스 로딩과 JIT 미적용 상태다. 이 랩의 첫 관측에서 gRPC 최초 호출이 REST 대비 3 배 느리게 나왔다 ([benchmark-design.md](benchmark-design.md) 참조).
- **페이로드 크기.** 필드 몇 개짜리 응답에서는 직렬화 비용 자체가 작아 차이가 묻힌다.
- **DB 유무.** DB 왕복이 수 밀리초면 프로토콜 오버헤드 차이는 상대적으로 미미해진다.

그래서 이 랩은 조건을 명시하고, 조건별 수치를 따로 보고한다.

## 4. 서비스 메시가 있어도 게이트웨이는 중복이 아니다

Envoy 에 `grpc_json_transcoder` 내장 필터가 있어 grpc-gateway 와 기능상 같은 일을 한다. 서비스 메시(Istio)의 데이터플레인이 Envoy 이므로, 메시를 쓰는 환경에서 게이트웨이를 따로 두면 프록시가 둘인 중복처럼 보인다.

**중복이 아니다. 계층이 다르다.** grpc-gateway 는 프록시가 아니라 **빌드 시 생성되는 코드**다. Go 에서는 게이트웨이 mux 를 gRPC 서버와 같은 바이너리에 컴파일하는 것이 표준 패턴이고, 두 goroutine 이 한 프로세스에서 포트를 나눠 연다. 배포 단위가 늘지 않는다. 이 랩이 Go 프로세스를 따로 띄운 것은 애플리케이션이 Java 라서 선택지가 없었기 때문이다.

| | grpc-gateway | Envoy transcoder |
|---|---|---|
| 실체 | 생성 코드 (라이브러리) | 프록시 내장 필터 |
| 배포 단위 | 늘지 않음 | 사이드카 설정 |
| 계약 주입 | 서비스와 함께 배포 | 컴파일된 descriptor 를 프록시 설정에 |
| 계약 변경 시 | 서비스만 재배포 | 메시 설정 갱신 |
| 소유권 | 서비스 팀 | 플랫폼 팀 |

**관리 리소스가 늘어나는 쪽은 메시 경로다.** 세 가지가 걸린다.

1. **1급 API 가 없다.** Istio 는 `EnvoyFilter` CRD 로만 가능하고, Istio 자신이 이를 escape hatch·alpha API 로 규정한다. xDS 생성 구현 세부에 결합되어 있어 번들 Envoy 버전이 오르면 조용히 깨질 수 있고, 특정 버전 고정을 권고한다.
2. **descriptor 배달이 까다롭다.** Envoy 는 `.proto` 를 직접 읽지 않고 컴파일된 descriptor set 이 필요하다. 파일 마운트는 사이드카 주입이 동적이지 않아 배포 타이밍 제어가 어렵고, base64 로 인라인하면 k8s 애노테이션 256KB 제한에 걸린다.
3. **계약마다 인프라 리소스가 생긴다.** 계약 변경이 인프라 변경 요청으로 바뀐다.

**그럼 메시는 왜 두는가.** 트랜스코딩과 무관하게 gRPC 자체가 메시를 필요로 한다. HTTP/2 는 커넥션을 오래 유지하며 다중화하므로 L4 로드밸런싱은 커넥션 단위가 되어 백엔드 편중이 생긴다. Envoy 가 요청 단위 L7 밸런싱으로 이를 푼다. 여기에 mTLS·재시도·타임아웃·관측이 붙는다. **역할이 겹치는 것이 아니라 기능 하나만 양쪽에 있고, 그중 인프라 쪽을 안 쓰는 구성이다.**

참고로 이 랩이 Envoy 를 넣은 목적은 트랜스코딩이 아니다. 직접 구현한 grpc-web 프록시가 표준과 같은 프레임을 내는지 대조하는 용도다.

출처: [Envoy grpc_json_transcoder](https://www.envoyproxy.io/docs/envoy/latest/configuration/http/http_filters/grpc_json_transcoder_filter) · [grpc-gateway 튜토리얼](https://grpc-ecosystem.github.io/grpc-gateway/docs/tutorials/adding_annotations/) · [tetratelabs/istio-tools grpc-transcoder](https://github.com/tetratelabs/istio-tools/blob/master/grpc-transcoder/README.md) · [Istio 1.9 upgrade notes](https://istio.io/latest/news/releases/1.9.x/announcing-1.9/upgrade-notes/) · [istio#18371](https://github.com/istio/istio/issues/18371)
