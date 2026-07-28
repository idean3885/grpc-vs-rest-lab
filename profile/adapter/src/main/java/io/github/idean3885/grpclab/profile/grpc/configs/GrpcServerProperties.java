package io.github.idean3885.grpclab.profile.grpc.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * gRPC 서버 설정.
 *
 * @param port gRPC 리스닝 포트. REST 포트(server.port)와 별개다.
 * @param reflectionEnabled 서버 리플렉션 노출 여부. grpcurl 이나 k6 가 .proto 없이 호출할 수 있게 한다.
 */
@ConfigurationProperties(prefix = "grpc.server")
public record GrpcServerProperties(int port, boolean reflectionEnabled) {}
