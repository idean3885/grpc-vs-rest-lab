package io.github.idean3885.grpclab.profile.rest.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 브라우저 관측용 CORS 허용.
 *
 * <p>관측 페이지(8000)에서 이 서버(8081)를 직접 호출하려면 필요하다. 게이트웨이와 grpc-web 프록시에도 같은 허용이 있으므로, 네 경로 모두 같은 조건에서
 * 비교된다.
 *
 * <p>{@code Content-Length} 를 노출한다. 브라우저 스크립트가 응답 크기를 읽어 표에 표시하기 때문이다. 노출하지 않으면 CORS 응답에서 이 헤더를 읽을
 * 수 없다.
 *
 * <p>로컬 랩 전용 설정이다. 운영에서 모든 오리진을 허용하면 안 된다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "POST", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("Content-Length", "Content-Type");
  }
}
