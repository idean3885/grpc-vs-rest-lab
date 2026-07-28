package io.github.idean3885.grpclab.profile.grpc;

import io.github.idean3885.grpclab.profile.ProfileQueryUseCase;
import io.github.idean3885.grpclab.profile.exception.ProfileNotFoundException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * gRPC 인바운드 어댑터.
 *
 * <p>REST 컨트롤러와 같은 {@link ProfileQueryUseCase} 를 호출한다. 어댑터가 하는 일은 프로토버프 메시지와 전송 객체를 옮기고, 도메인 예외를
 * gRPC 상태 코드로 번역하는 것뿐이다.
 *
 * <p>예외 번역이 REST 와 다른 점에 주의한다. REST 는 HTTP 상태 코드를 쓰지만 gRPC 는 자체 상태 코드 체계를 쓰고, 그 값은 HTTP/2 trailer 에
 * 담긴다. 브라우저에서 순수 gRPC 를 쓸 수 없는 이유가 여기서 나온다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ProfileGrpcService extends ProfileServiceGrpc.ProfileServiceImplBase {

  private final ProfileQueryUseCase profileQueryUseCase;

  @Override
  public void getProfile(GetProfileRequest request, StreamObserver<Profile> responseObserver) {
    try {
      var data = profileQueryUseCase.getBy(request.getUserId());
      responseObserver.onNext(ProfileProtoMapper.toProto(data));
      responseObserver.onCompleted();
    } catch (ProfileNotFoundException e) {
      responseObserver.onError(
          Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void listProfiles(
      ListProfilesRequest request, StreamObserver<ListProfilesResponse> responseObserver) {
    var list = profileQueryUseCase.listBy(request.getSize());
    var response =
        ListProfilesResponse.newBuilder()
            .addAllProfiles(list.profiles().stream().map(ProfileProtoMapper::toProto).toList())
            .setTotal(list.total())
            .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  /**
   * 서버 스트리밍.
   *
   * <p>다건을 한 메시지로 모으지 않고 건별로 흘려보낸다. 응답 전체를 메모리에 쌓지 않아도 되는 대신, 메시지마다 5 바이트 프레임 헤더가 붙는다. 건수가 많고 건당
   * 크기가 작으면 이 헤더 비용이 누적된다.
   */
  @Override
  public void streamProfiles(
      ListProfilesRequest request, StreamObserver<Profile> responseObserver) {
    profileQueryUseCase
        .listBy(request.getSize())
        .profiles()
        .forEach(data -> responseObserver.onNext(ProfileProtoMapper.toProto(data)));
    responseObserver.onCompleted();
  }
}
