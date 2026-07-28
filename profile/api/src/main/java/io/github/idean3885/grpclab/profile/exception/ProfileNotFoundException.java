package io.github.idean3885.grpclab.profile.exception;

/** 조회 대상 프로필이 없는 경우. */
public class ProfileNotFoundException extends RuntimeException {

  public ProfileNotFoundException(String userId) {
    super("profile not found : userId=" + userId);
  }
}
