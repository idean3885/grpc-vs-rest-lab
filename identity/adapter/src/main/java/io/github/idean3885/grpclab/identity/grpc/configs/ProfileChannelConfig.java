package io.github.idean3885.grpclab.identity.grpc.configs;

import io.github.idean3885.grpclab.identity.configs.ProfileClientProperties;
import io.github.idean3885.grpclab.profile.grpc.ProfileServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC 채널 설정.
 *
 * <p>채널은 애플리케이션 수명 동안 하나만 두고 재사용한다. 채널이 커넥션과 HTTP/2 스트림 멀티플렉싱을 관리하므로, 요청마다 채널을 만들면 gRPC 쪽에만 커넥션 설정
 * 비용이 반복 계상된다. REST 커넥션 풀과 대응되는 조건이다.
 *
 * <p>{@code usePlaintext()} 로 TLS 를 끈다. 로컬 랩에서 TLS 를 켜면 핸드셰이크 비용이 프로토콜 차이에 섞이고, REST 쪽도 같은 조건으로 맞춰야
 * 하므로 양쪽 모두 평문으로 둔다. 실제 운영에서는 서비스 메시가 mTLS 를 담당하는 구성이 흔하다.
 */
@Slf4j
@Configuration
public class ProfileChannelConfig {

  @Bean(destroyMethod = "shutdownNow")
  public ManagedChannel profileChannel(ProfileClientProperties properties) {
    var channel =
        ManagedChannelBuilder.forAddress(properties.grpcHost(), properties.grpcPort())
            .usePlaintext()
            .build();
    log.info(
        "[grpc] channel created : host={}, port={}", properties.grpcHost(), properties.grpcPort());
    return channel;
  }

  @Bean
  public ProfileServiceGrpc.ProfileServiceBlockingStub profileStub(ManagedChannel profileChannel) {
    return ProfileServiceGrpc.newBlockingStub(profileChannel);
  }
}
