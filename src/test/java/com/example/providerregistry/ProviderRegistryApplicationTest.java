package com.example.providerregistry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProviderRegistryApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ClientHttpRequestFactory requestFactory;

    @Autowired
    private HttpClientSettings httpClientSettings;

    @Test
    void usesRestClientBuilderFromBootAutoConfiguration() {
        assertThat(applicationContext.containsBean("restClientBuilder")).isTrue();
        assertThat(applicationContext.isPrototype("restClientBuilder")).isTrue();
    }

    @Test
    void usesJdkHttpClientWithConfiguredTimeouts() {
        assertThat(requestFactory).isInstanceOf(JdkClientHttpRequestFactory.class);
        assertThat(httpClientSettings.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(httpClientSettings.readTimeout()).isEqualTo(Duration.ofSeconds(5));
    }
}
