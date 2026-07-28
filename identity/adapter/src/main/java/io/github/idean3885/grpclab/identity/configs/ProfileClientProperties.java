package io.github.idean3885.grpclab.identity.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프로필 서비스 접속 설정.
 *
 * @param restBaseUrl REST 엔드포인트 base URL
 * @param grpcHost gRPC 호스트
 * @param grpcPort gRPC 포트
 * @param maxConnections REST 커넥션 풀 상한. gRPC 채널이 커넥션을 재사용하므로 REST 쪽도 풀을 명시해 조건을 맞춘다.
 */
@ConfigurationProperties(prefix = "profile.client")
public record ProfileClientProperties(
    String restBaseUrl, String grpcHost, int grpcPort, int maxConnections) {}
