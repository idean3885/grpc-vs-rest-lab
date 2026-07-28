package io.github.idean3885.grpclab.identity.grpc;

import io.github.idean3885.grpclab.identity.enums.Transport;
import io.github.idean3885.grpclab.identity.ports.ProfileLookupPort;
import io.github.idean3885.grpclab.profile.grpc.ListProfilesRequest;
import io.github.idean3885.grpclab.profile.grpc.ProfileServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * gRPC 아웃바운드 어댑터.
 *
 * <p>{@link ProfileLookupRestPortImpl} 과 같은 포트를 구현한다. 호출 코드 분량은 REST 쪽과 비슷하지만, 요청 메시지를 빌더로 만들고 응답을
 * 프로토버프 객체로 받는다는 점이 다르다.
 *
 * <p>blocking 스텁을 쓴다. 비동기 스텁이 더 빠른 경우가 있으나, 비교 대상인 RestClient 가 동기이므로 같은 호출 모델로 맞췄다.
 */
@RequiredArgsConstructor
@Component
public class ProfileLookupGrpcPortImpl implements ProfileLookupPort {

  private final ProfileServiceGrpc.ProfileServiceBlockingStub profileStub;

  @Override
  public Transport transport() {
    return Transport.GRPC;
  }

  @Override
  public LookupResult list(int size) {
    var request = ListProfilesRequest.newBuilder().setSize(size).build();
    var response = profileStub.listProfiles(request);
    var sample = response.getProfilesCount() == 0 ? null : response.getProfiles(0).getNickname();
    return new LookupResult(response.getTotal(), sample);
  }
}
