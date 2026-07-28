package io.github.idean3885.grpclab.identity.ports;

import io.github.idean3885.grpclab.identity.enums.Transport;

/**
 * 프로필 서비스 조회 아웃바운드 포트.
 *
 * <p>구현이 REST 와 gRPC 두 개다. 도메인은 어느 쪽이 주입됐는지 모른다. 프로토콜을 바꾸는 작업이 이 인터페이스 구현체를 하나 더 넣는 일로 끝나는지가 헥사고날
 * 구조의 실익을 판정하는 기준이다.
 *
 * <p>identity 모듈은 profile 모듈에 컴파일 의존하지 않는다. 서비스 경계를 넘는 데이터는 이 포트가 정의한 {@link LookupResult} 로만 들어온다.
 */
public interface ProfileLookupPort {

  /** 이 구현이 담당하는 전송 방식. 서비스가 요청받은 전송으로 구현을 고를 때 쓴다. */
  Transport transport();

  /**
   * 프로필 목록을 조회한다.
   *
   * @param size 조회 건수
   * @return 조회 결과
   */
  LookupResult list(int size);

  /**
   * 조회 결과.
   *
   * @param count 받아온 건수
   * @param sampleNickname 첫 건의 닉네임. 응답 본문이 실제로 역직렬화됐는지 확인하는 용도.
   */
  record LookupResult(int count, String sampleNickname) {}
}
