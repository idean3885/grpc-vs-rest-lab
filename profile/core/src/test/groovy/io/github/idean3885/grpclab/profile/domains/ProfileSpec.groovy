package io.github.idean3885.grpclab.profile.domains

import spock.lang.Specification

/**
 * 인증 판정은 저장값이 아니라 도메인에서 파생되는 규칙이다.
 *
 * 이 규칙이 도메인에 있으므로 REST 응답과 gRPC 응답이 같은 판정을 거친다.
 * 규칙이 어댑터로 새면 프로토콜별로 결과가 갈릴 수 있고, 그것을 막는 것이 이 테스트다.
 */
class ProfileSpec extends Specification {

  def "신뢰 점수 70 을 경계로 인증 여부가 갈린다"() {
    expect:
    profileWith(trustScore).verified() == verified

    where:
    trustScore || verified
    0          || false
    69         || false
    70         || true
    71         || true
    100        || true
  }

  private static Profile profileWith(int trustScore) {
    new Profile('user-00001', '사용자00001', '지역A', trustScore, 1_700_000_000_000L)
  }
}
