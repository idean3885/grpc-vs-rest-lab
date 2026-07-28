package io.github.idean3885.grpclab.profile

import io.github.idean3885.grpclab.profile.domains.Profile
import io.github.idean3885.grpclab.profile.exception.ProfileNotFoundException
import io.github.idean3885.grpclab.profile.ports.ProfileRepository
import spock.lang.Specification

/**
 * 유스케이스 단위 테스트.
 *
 * 스프링 컨텍스트도, 서버 기동도, 네트워크도 없다. 저장소 포트를 대역으로 바꾸는 것으로 끝난다.
 * 이 클래스를 REST 컨트롤러와 gRPC 서비스가 함께 호출하므로, 여기서 검증한 동작이 두 프로토콜에 공통으로 적용된다.
 */
class ProfileQueryServiceSpec extends Specification {

  ProfileRepository profileRepository = Mock()
  ProfileQueryService sut = new ProfileQueryService(profileRepository)

  def "단건 조회 결과에 도메인 파생 규칙이 실린다"() {
    given:
    profileRepository.findById('user-00001') >> Optional.of(profile('user-00001', 70))

    when:
    def data = sut.getBy('user-00001')

    then:
    data.userId() == 'user-00001'
    data.trustScore() == 70
    data.verified()
  }

  def "저장소에 없는 사용자는 조회 예외로 구분한다"() {
    given:
    profileRepository.findById('absent') >> Optional.empty()

    when:
    sut.getBy('absent')

    then:
    def e = thrown(ProfileNotFoundException)
    e.message.contains('absent')
  }

  def "다건 조회는 받은 건수를 total 로 채우고 순서를 유지한다"() {
    given:
    profileRepository.findAll(3) >> [
      profile('user-1', 10),
      profile('user-2', 70),
      profile('user-3', 90)
    ]

    when:
    def data = sut.listBy(3)

    then:
    data.total() == 3
    data.profiles()*.userId() == ['user-1', 'user-2', 'user-3']
    data.profiles()*.verified() == [false, true, true]
  }

  def "조회 건수 0 은 빈 목록으로 돌려준다"() {
    given:
    profileRepository.findAll(0) >> []

    when:
    def data = sut.listBy(0)

    then:
    data.total() == 0
    data.profiles().isEmpty()
  }

  private static Profile profile(String userId, int trustScore) {
    new Profile(userId, "사용자-${userId}", '지역A', trustScore, 1_700_000_000_000L)
  }
}
