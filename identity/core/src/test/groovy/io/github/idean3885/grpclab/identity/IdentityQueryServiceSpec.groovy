package io.github.idean3885.grpclab.identity

import io.github.idean3885.grpclab.identity.enums.Transport
import io.github.idean3885.grpclab.identity.ports.ProfileLookupPort
import spock.lang.Specification

/**
 * 전송 방식 선택이 조건문이 아니라 구현 선택으로 표현되는지 검증한다.
 *
 * 이 랩의 비교가 성립하는 전제이기도 하다. REST 와 gRPC 가 같은 유스케이스를 통과해야
 * 측정된 차이를 전송 방식 차이로 읽을 수 있다.
 */
class IdentityQueryServiceSpec extends Specification {

  def "요청받은 전송을 담당하는 구현으로 위임한다"() {
    given:
    def sut = new IdentityQueryService([
      fixedPort(Transport.REST, 3, 'rest-님'),
      fixedPort(Transport.GRPC, 3, 'grpc-님')
    ])

    when:
    def bundle = sut.me('user-00001', 3, requested)

    then:
    bundle.transport() == requested
    bundle.sampleNickname() == sample
    bundle.profileCount() == 3
    bundle.userId() == 'user-00001'

    where:
    requested      || sample
    Transport.REST || 'rest-님'
    Transport.GRPC || 'grpc-님'
  }

  def "포트 주입 순서는 선택 결과를 바꾸지 않는다"() {
    given:
    def reversed = new IdentityQueryService([
      fixedPort(Transport.GRPC, 1, 'grpc-님'),
      fixedPort(Transport.REST, 1, 'rest-님')
    ])

    expect:
    reversed.me('user-00001', 1, Transport.REST).sampleNickname() == 'rest-님'
    reversed.me('user-00001', 1, Transport.GRPC).sampleNickname() == 'grpc-님'
  }

  def "주입되지 않은 전송을 요청하면 거부한다"() {
    given: "REST 구현만 등록된 상태"
    def sut = new IdentityQueryService([fixedPort(Transport.REST, 1, 'rest-님')])

    when:
    sut.me('user-00001', 1, Transport.GRPC)

    then:
    def e = thrown(IllegalArgumentException)
    e.message.contains('GRPC')
  }

  def "상류 호출 구간만 측정해 응답에 담는다"() {
    given: "상류가 20ms 지연되는 포트"
    def sut = new IdentityQueryService([new DelayedPort(Transport.REST, 20L)])

    when:
    def bundle = sut.me('user-00001', 1, Transport.REST)

    then: "지연 시간 이상이 기록된다"
    bundle.upstreamElapsedMicros() >= 10_000L
  }

  private static ProfileLookupPort fixedPort(Transport transport, int count, String sampleNickname) {
    new FixedPort(transport, new ProfileLookupPort.LookupResult(count, sampleNickname))
  }

  /** 고정 결과를 돌려주는 대역. */
  private static class FixedPort implements ProfileLookupPort {

    private final Transport transport
    private final ProfileLookupPort.LookupResult result

    FixedPort(Transport transport, ProfileLookupPort.LookupResult result) {
      this.transport = transport
      this.result = result
    }

    @Override
    Transport transport() {
      transport
    }

    @Override
    ProfileLookupPort.LookupResult list(int size) {
      result
    }
  }

  /** 상류 지연을 재현하는 대역. */
  private static class DelayedPort implements ProfileLookupPort {

    private final Transport transport
    private final long delayMillis

    DelayedPort(Transport transport, long delayMillis) {
      this.transport = transport
      this.delayMillis = delayMillis
    }

    @Override
    Transport transport() {
      transport
    }

    @Override
    ProfileLookupPort.LookupResult list(int size) {
      Thread.sleep(delayMillis)
      new ProfileLookupPort.LookupResult(size, 'delayed-님')
    }
  }
}
