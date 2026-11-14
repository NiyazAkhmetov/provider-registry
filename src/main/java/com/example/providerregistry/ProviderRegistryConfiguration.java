package com.example.providerregistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

/**
 * Spring-конфигурация сервиса.
 */
@Configuration(proxyBeanMethods = false)
public class ProviderRegistryConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ProviderRegistryConfiguration.class);

    /**
     * Используется {@link RestClient.Builder}, который создаёт Spring Boot.
     * Благодаря этому работают свойства {@code spring.http.clients.*}
     * (таймауты, редиректы, выбор HTTP-клиента) и общие кастомайзеры.
     */
    @Bean
    ProviderPhoneRegistry providerPhoneRegistry(RestClient.Builder restClientBuilder) {
        return new ProviderPhoneRegistry(restClientBuilder);
    }

    @Bean
    ProviderJsonLoader providerJsonLoader(ObjectMapper objectMapper) {
        return new ProviderJsonLoader(objectMapper);
    }

    /**
     * Если задано свойство {@code providers.file}, загружает поставщиков из файла при старте.
     */
    @Bean
    ApplicationRunner providersFromFileLoader(
            ProviderPhoneRegistry registry,
            ProviderJsonLoader loader,
            @Value("${providers.file:}") String providersFile) {

        return args -> {
            if (StringUtils.hasText(providersFile)) {
                // Fail-fast: если файл задан, но не читается, приложение не должно
                // стартовать с пустым или частично загруженным реестром.
                List<Provider> providers = loader.load(Path.of(providersFile));
                registry.setProviders(providers);
                log.info("Loaded {} provider(s) from {}", providers.size(), providersFile);
            }
        };
    }
}
