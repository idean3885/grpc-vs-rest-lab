package io.github.idean3885.grpclab.identity.enums;

/**
 * 서비스 간 호출에 쓰는 전송 방식.
 *
 * <p>이 랩의 비교 축이다. 같은 유스케이스를 어느 전송으로 처리했는지만 다르게 두고, 그 외 조건은 모두 같게 유지한다.
 */
public enum Transport {

  /** RestClient 기반 선언형 HTTP 인터페이스. HTTP/1.1 + JSON. */
  REST,

  /** 생성 스텁 기반 blocking 호출. HTTP/2 + 프로토버프. */
  GRPC
}
