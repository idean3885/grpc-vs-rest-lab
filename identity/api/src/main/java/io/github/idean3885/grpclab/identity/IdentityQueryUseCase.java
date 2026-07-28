package io.github.idean3885.grpclab.identity;

import io.github.idean3885.grpclab.identity.data.IdentityBundleData;
import io.github.idean3885.grpclab.identity.enums.Transport;

/** 호출 측 인바운드 포트. */
public interface IdentityQueryUseCase {

  /**
   * 프로필 서비스를 호출해 결과 요약을 돌려준다.
   *
   * <p>응답에 프로필 원본을 담지 않는 이유는, 클라이언트 to identity 구간의 직렬화 비용이 측정 대상인 identity to profile 구간의 차이를 덮지
   * 않게 하기 위해서다.
   *
   * @param userId 사용자 식별자
   * @param size 프로필 서비스에서 가져올 건수
   * @param transport 사용할 전송 방식
   * @return 호출 결과 요약
   */
  IdentityBundleData me(String userId, int size, Transport transport);
}
