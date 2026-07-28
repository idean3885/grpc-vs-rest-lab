package io.github.idean3885.grpclab.identity.web;

import io.github.idean3885.grpclab.identity.IdentityQueryUseCase;
import io.github.idean3885.grpclab.identity.data.IdentityBundleData;
import io.github.idean3885.grpclab.identity.enums.Transport;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 호출 측 인바운드 어댑터.
 *
 * <p>전송 방식을 경로로 분리했다. 한 프로세스에서 두 경로를 번갈아 때릴 수 있으므로, JVM 워밍업 상태·GC 조건·힙 점유가 동일한 상태에서 REST 와 gRPC 를
 * 비교할 수 있다. 서버를 각각 띄워 비교하면 그 조건들이 달라진다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/me")
public class IdentityRestController {

  private final IdentityQueryUseCase identityQueryUseCase;

  @GetMapping("/rest")
  public IdentityBundleData viaRest(
      @RequestParam(defaultValue = "user-00001") String userId,
      @RequestParam(defaultValue = "1") int size) {
    return identityQueryUseCase.me(userId, size, Transport.REST);
  }

  @GetMapping("/grpc")
  public IdentityBundleData viaGrpc(
      @RequestParam(defaultValue = "user-00001") String userId,
      @RequestParam(defaultValue = "1") int size) {
    return identityQueryUseCase.me(userId, size, Transport.GRPC);
  }
}
