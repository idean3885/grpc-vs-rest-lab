package io.github.idean3885.grpclab.profile.ports;

import io.github.idean3885.grpclab.profile.domains.Profile;
import java.util.List;
import java.util.Optional;

/** 프로필 저장소 아웃바운드 포트. */
public interface ProfileRepository {

  Optional<Profile> findById(String userId);

  /**
   * 앞에서 size 건을 조회한다.
   *
   * @param size 조회 건수
   * @return 프로필 목록
   */
  List<Profile> findAll(int size);
}
