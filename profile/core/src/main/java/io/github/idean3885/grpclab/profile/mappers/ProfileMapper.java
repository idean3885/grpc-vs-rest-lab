package io.github.idean3885.grpclab.profile.mappers;

import io.github.idean3885.grpclab.profile.data.ProfileData;
import io.github.idean3885.grpclab.profile.domains.Profile;

/** 도메인 to 전송 객체 변환. */
public final class ProfileMapper {

  private ProfileMapper() {}

  public static ProfileData toData(Profile profile) {
    return ProfileData.builder()
        .userId(profile.userId())
        .nickname(profile.nickname())
        .region(profile.region())
        .trustScore(profile.trustScore())
        .verified(profile.verified())
        .joinedAt(profile.joinedAt())
        .build();
  }
}
