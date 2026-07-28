package io.github.idean3885.grpclab.identity.rest.configs;

import io.github.idean3885.grpclab.identity.configs.ProfileClientProperties;
import io.github.idean3885.grpclab.identity.rest.ProfileHttpClient;
import java.time.Duration;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * REST 클라이언트 설정.
 *
 * <p>커넥션 풀을 명시적으로 잡는다. 기본 요청 팩토리는 요청마다 커넥션을 새로 열 수 있고, 그 상태로 gRPC 와 비교하면 REST 쪽에 TCP 핸드셰이크 비용이 반복
 * 계상되어 비교가 성립하지 않는다. gRPC 는 채널 하나로 커넥션을 유지하며 여러 요청을 멀티플렉싱하므로, REST 쪽도 keep-alive 와 풀 재사용을 보장한 뒤
 * 측정해야 한다.
 *
 * <p>이 랩에서 확인하려는 것은 "프로토콜 차이"이지 "설정을 덜 한 REST" 가 아니다.
 */
@Configuration
public class ProfileHttpClientConfig {

  @Bean
  public ProfileHttpClient profileHttpClient(ProfileClientProperties properties) {
    var connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(properties.maxConnections())
            .setMaxConnPerRoute(properties.maxConnections())
            .build();

    var httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
    var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
    requestFactory.setConnectTimeout(Duration.ofSeconds(2));

    var restClient =
        RestClient.builder()
            .baseUrl(properties.restBaseUrl())
            .requestFactory(requestFactory)
            .build();

    var adapter = RestClientAdapter.create(restClient);
    return HttpServiceProxyFactory.builderFor(adapter)
        .build()
        .createClient(ProfileHttpClient.class);
  }
}
