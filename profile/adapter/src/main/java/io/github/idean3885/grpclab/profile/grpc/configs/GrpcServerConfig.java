package io.github.idean3885.grpclab.profile.grpc.configs;

import io.github.idean3885.grpclab.profile.grpc.ProfileGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC 서버 생명주기 설정.
 *
 * <p>스타터를 쓰지 않고 grpc-java 를 직접 빈으로 등록한다. 공식 Spring gRPC 는 Spring Boot 4.1 라인 전용이고, 커뮤니티
 * 스타터(net.devh)는 Spring Boot 3.2 기준에서 유지가 멈췄다. 직접 등록하면 서버가 어느 스레드 풀에서 돌고 언제 시작·종료되는지가 코드에 그대로 드러난다.
 *
 * <p>REST 서버(톰캣)와 별개 포트에서 함께 뜬다. 두 인바운드 어댑터가 같은 JVM·같은 도메인 코어를 쓰므로, 측정된 차이에 JVM 워밍업 상태나 GC 조건 차이가
 * 섞이지 않는다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcServerConfig {

  @Bean(destroyMethod = "shutdownNow")
  public Server grpcServer(GrpcServerProperties properties, ProfileGrpcService profileGrpcService)
      throws IOException {
    var builder = ServerBuilder.forPort(properties.port()).addService(profileGrpcService);

    if (properties.reflectionEnabled()) {
      builder.addService(ProtoReflectionServiceV1.newInstance());
    }

    var server = builder.build().start();
    log.info(
        "[grpc] server started : port={}, reflection={}",
        properties.port(),
        properties.reflectionEnabled());
    return server;
  }
}
