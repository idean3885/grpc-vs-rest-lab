package io.github.idean3885.grpclab.profile.data;

import lombok.Builder;

/**
 * 프로필 전송 객체.
 *
 * <p>필드 구성을 proto 의 {@code profile.v1.Profile} 과 1:1 로 맞췄다. JSON 과 프로토버프의 크기 차이를 비교할 때 필드 개수·타입이
 * 달라지면 비교가 성립하지 않는다.
 */
@Builder
public record ProfileData(
    String userId,
    String nickname,
    String region,
    int trustScore,
    boolean verified,
    long joinedAt) {}
