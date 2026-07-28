package io.github.idean3885.grpclab.profile.rest;

import io.github.idean3885.grpclab.profile.ProfileQueryUseCase;
import io.github.idean3885.grpclab.profile.data.ProfileData;
import io.github.idean3885.grpclab.profile.data.ProfileListData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST 인바운드 어댑터.
 *
 * <p>경로를 proto 의 {@code google.api.http} 옵션과 동일하게 맞췄다. 같은 경로를 REST 서버와 grpc-gateway 가 각각 제공하므로,
 * 클라이언트 입장에서 두 경로를 구분 없이 비교할 수 있다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/profiles")
public class ProfileRestController {

  private final ProfileQueryUseCase profileQueryUseCase;

  @GetMapping("/{userId}")
  public ProfileData getProfile(@PathVariable String userId) {
    return profileQueryUseCase.getBy(userId);
  }

  @GetMapping
  public ProfileListData listProfiles(@RequestParam(defaultValue = "1") int size) {
    return profileQueryUseCase.listBy(size);
  }
}
