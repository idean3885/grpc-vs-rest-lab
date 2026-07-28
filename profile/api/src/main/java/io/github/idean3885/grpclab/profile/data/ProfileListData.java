package io.github.idean3885.grpclab.profile.data;

import java.util.List;

/** 프로필 목록 전송 객체. */
public record ProfileListData(List<ProfileData> profiles, int total) {

  public static ProfileListData from(List<ProfileData> profiles) {
    return new ProfileListData(profiles, profiles.size());
  }
}
