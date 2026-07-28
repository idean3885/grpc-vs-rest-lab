package io.github.idean3885.grpclab.identity.data;

import io.github.idean3885.grpclab.identity.enums.Transport;
import lombok.Builder;

/**
 * 호출 결과 요약.
 *
 * @param userId 사용자 식별자
 * @param transport 사용한 전송 방식
 * @param profileCount 받아온 프로필 건수
 * @param upstreamElapsedMicros identity to profile 구간에서 측정한 왕복 시간 (마이크로초)
 * @param sampleNickname 응답이 실제로 채워졌는지 확인하는 표본. 직렬화 생략 최적화를 방지한다.
 */
@Builder
public record IdentityBundleData(
    String userId,
    Transport transport,
    int profileCount,
    long upstreamElapsedMicros,
    String sampleNickname) {}
