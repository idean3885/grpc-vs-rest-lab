package io.github.idean3885.grpclab.profile.grpc

import io.github.idean3885.grpclab.profile.data.ProfileData
import spock.lang.Specification

/**
 * 프로토버프 경계 테스트.
 *
 * 도메인 타입과 proto 타입이 두 벌로 갈리는 것이 gRPC 채택의 대가다. 필드를 추가할 때 양쪽을 손대야 하고,
 * 한쪽만 손대면 값이 조용히 빠진다. 그 누락을 잡는 것이 첫 번째 테스트다.
 *
 * 두 번째 테스트는 리드미의 크기 비교 근거를 코드로 고정한다.
 * proto3 는 기본값을 와이어에 싣지 않고, JSON 은 키를 그대로 남긴다.
 */
class ProfileProtoMapperSpec extends Specification {

  def "전송 객체의 여섯 필드를 프로토버프 메시지로 빠짐없이 옮긴다"() {
    given:
    def data = ProfileData.builder()
        .userId('user-00001')
        .nickname('사용자00001')
        .region('지역A')
        .trustScore(70)
        .verified(true)
        .joinedAt(1_700_000_000_000L)
        .build()

    when:
    def proto = ProfileProtoMapper.toProto(data)

    then:
    proto.userId == 'user-00001'
    proto.nickname == '사용자00001'
    proto.region == '지역A'
    proto.trustScore == 70
    proto.verified
    proto.joinedAt == 1_700_000_000_000L
  }

  def "기본값을 명시해도 와이어에 실리지 않는다"() {
    given: "문자열 세 필드만 채운 메시지"
    def base = Profile.newBuilder()
        .setUserId('user-00001')
        .setNickname('사용자00001')
        .setRegion('지역A')

    when: "정수 필드를 기본값 0 으로 명시하면"
    def withZero = base.clone().setTrustScore(0).build()

    then: "설정하지 않은 것과 직렬화 크기가 같다"
    withZero.serializedSize == base.clone().build().serializedSize

    when: "0 이 아닌 값을 넣으면"
    def withValue = base.clone().setTrustScore(70).build()

    then: "태그 1바이트 + varint 1바이트만 늘어난다"
    withValue.serializedSize == withZero.serializedSize + 2
  }

  def "왕복 직렬화로 값이 보존된다"() {
    given:
    def origin = ProfileProtoMapper.toProto(
        ProfileData.builder()
            .userId('user-00001')
            .nickname('사용자00001')
            .region('지역A')
            .trustScore(70)
            .verified(true)
            .joinedAt(1_700_000_000_000L)
            .build())

    when:
    def restored = Profile.parseFrom(origin.toByteArray())

    then:
    restored == origin
  }
}
