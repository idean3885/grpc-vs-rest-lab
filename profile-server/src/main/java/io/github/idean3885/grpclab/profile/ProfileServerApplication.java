package io.github.idean3885.grpclab.profile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 피호출 서비스.
 *
 * <p>같은 도메인 코어를 REST(8081)와 gRPC(9090) 두 포트로 동시에 노출한다.
 */
@SpringBootApplication(scanBasePackages = "io.github.idean3885.grpclab.profile")
public class ProfileServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProfileServerApplication.class, args);
  }
}
