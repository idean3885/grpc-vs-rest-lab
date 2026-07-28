package io.github.idean3885.grpclab.profile.grpc;

import io.github.idean3885.grpclab.profile.data.ProfileData;

/**
 * 전송 객체 to 프로토버프 메시지 변환.
 *
 * <p>프로토버프 메시지는 빌더로만 만든다. 필드를 지정하지 않으면 기본값(0, 빈 문자열, false)이 들어가고, proto3 에서는 기본값이 와이어에 실리지 않는다. 이
 * 성질이 JSON 대비 크기 차이를 만드는 요인 중 하나다.
 */
final class ProfileProtoMapper {

  private ProfileProtoMapper() {}

  static Profile toProto(ProfileData data) {
    return Profile.newBuilder()
        .setUserId(data.userId())
        .setNickname(data.nickname())
        .setRegion(data.region())
        .setTrustScore(data.trustScore())
        .setVerified(data.verified())
        .setJoinedAt(data.joinedAt())
        .build();
  }
}
