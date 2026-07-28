package io.github.idean3885.grpclab.identity.rest;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 프로필 서비스 REST 클라이언트.
 *
 * <p>Spring 6.1+ 선언형 HTTP 인터페이스다. 구현체를 작성하지 않고 인터페이스만 선언하면 {@code HttpServiceProxyFactory} 가
 * RestClient 기반 프록시를 만들어 준다.
 *
 * <p>gRPC 의 생성 스텁과 대비되는 지점이다. gRPC 는 .proto 에서 스텁을 컴파일 타임에 생성하고, 이쪽은 런타임에 프록시를 만든다. 호출하는 코드 모양은 거의
 * 같지만, 계약을 검증하는 시점이 다르다. 응답 필드가 바뀌었을 때 gRPC 는 재생성 시 컴파일이 깨지고, 이쪽은 역직렬화 시점까지 조용하다.
 */
@HttpExchange("/v1/profiles")
public interface ProfileHttpClient {

  @GetExchange
  ProfileListResponse list(@RequestParam int size);

  /** 프로필 서비스 응답. profile 모듈에 컴파일 의존하지 않기 위해 호출 측에서 다시 선언한다. */
  record ProfileListResponse(List<ProfileResponse> profiles, int total) {}

  record ProfileResponse(
      String userId,
      String nickname,
      String region,
      int trustScore,
      boolean verified,
      long joinedAt) {}
}
