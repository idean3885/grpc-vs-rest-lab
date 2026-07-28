package io.github.idean3885.grpclab.identity.rest;

import io.github.idean3885.grpclab.identity.enums.Transport;
import io.github.idean3885.grpclab.identity.ports.ProfileLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** REST 아웃바운드 어댑터. */
@RequiredArgsConstructor
@Component
public class ProfileLookupRestPortImpl implements ProfileLookupPort {

  private final ProfileHttpClient profileHttpClient;

  @Override
  public Transport transport() {
    return Transport.REST;
  }

  @Override
  public LookupResult list(int size) {
    var response = profileHttpClient.list(size);
    var sample = response.profiles().isEmpty() ? null : response.profiles().get(0).nickname();
    return new LookupResult(response.total(), sample);
  }
}
