package io.github.idean3885.grpclab.profile.domains;

/**
 * 프로필 도메인.
 *
 * <p>인증 여부는 저장값이 아니라 신뢰 점수에서 파생되는 규칙이다. 이 규칙이 도메인에 있으므로 REST 응답과 gRPC 응답이 같은 판정을 거친다. 프로토콜을 바꿔도 도메인
 * 규칙은 재구현되지 않는다.
 */
public record Profile(
    String userId, String nickname, String region, int trustScore, long joinedAt) {

  /** 인증 배지 부여 기준 점수. */
  private static final int VERIFIED_THRESHOLD = 70;

  public boolean verified() {
    return trustScore >= VERIFIED_THRESHOLD;
  }
}
