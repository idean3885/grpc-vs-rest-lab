package io.github.idean3885.grpclab.identity;

import io.github.idean3885.grpclab.identity.configs.ProfileClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 호출 서비스.
 *
 * <p>프로필 서비스를 REST 와 gRPC 두 방식으로 호출한다. 두 아웃바운드 어댑터가 같은 프로세스에 함께 뜬다.
 */
@SpringBootApplication(scanBasePackages = "io.github.idean3885.grpclab.identity")
@EnableConfigurationProperties(ProfileClientProperties.class)
public class IdentityServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(IdentityServerApplication.class, args);
  }
}
