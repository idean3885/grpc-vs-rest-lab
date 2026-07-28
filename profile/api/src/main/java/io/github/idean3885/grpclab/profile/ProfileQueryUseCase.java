package io.github.idean3885.grpclab.profile;

import io.github.idean3885.grpclab.profile.data.ProfileData;
import io.github.idean3885.grpclab.profile.data.ProfileListData;

/**
 * 프로필 조회 인바운드 포트.
 *
 * <p>REST 컨트롤러와 gRPC 서비스가 같은 포트를 호출한다. 두 인바운드 어댑터가 공유하는 지점이 이 인터페이스뿐이므로, 벤치마크에서 측정되는 차이는 프로토콜 계층에서만
 * 발생한다.
 */
public interface ProfileQueryUseCase {

  /**
   * 단건 조회.
   *
   * @param userId 사용자 식별자
   * @return 프로필
   * @throws io.github.idean3885.grpclab.profile.exception.ProfileNotFoundException 대상이 없는 경우
   */
  ProfileData getBy(String userId);

  /**
   * 다건 조회. size 로 응답 페이로드 크기를 조절한다.
   *
   * @param size 조회 건수
   * @return 프로필 목록
   */
  ProfileListData listBy(int size);
}
