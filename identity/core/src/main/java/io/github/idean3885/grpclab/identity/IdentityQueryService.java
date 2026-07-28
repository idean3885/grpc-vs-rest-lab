package io.github.idean3885.grpclab.identity;

import io.github.idean3885.grpclab.identity.data.IdentityBundleData;
import io.github.idean3885.grpclab.identity.enums.Transport;
import io.github.idean3885.grpclab.identity.ports.ProfileLookupPort;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * 호출 측 유스케이스.
 *
 * <p>전송 방식별 구현을 주입받아 요청된 전송으로 위임한다. 분기가 {@code if (grpc)} 같은 조건문이 아니라 구현 선택으로 표현되므로, 전송을 추가할 때 이
 * 클래스는 수정되지 않는다.
 */
@Service
public class IdentityQueryService implements IdentityQueryUseCase {

  private final Map<Transport, ProfileLookupPort> ports = new EnumMap<>(Transport.class);

  public IdentityQueryService(List<ProfileLookupPort> profileLookupPorts) {
    profileLookupPorts.forEach(port -> ports.put(port.transport(), port));
  }

  @Override
  public IdentityBundleData me(String userId, int size, Transport transport) {
    var port = ports.get(transport);
    if (port == null) {
      throw new IllegalArgumentException("unsupported transport : " + transport);
    }

    var startedAt = System.nanoTime();
    var result = port.list(size);
    var elapsedMicros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startedAt);

    return IdentityBundleData.builder()
        .userId(userId)
        .transport(transport)
        .profileCount(result.count())
        .upstreamElapsedMicros(elapsedMicros)
        .sampleNickname(result.sampleNickname())
        .build();
  }
}
