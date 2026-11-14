package com.example.providerregistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderTest {

    private static final String BASE_URL = "https://provider.example";
    private static final String API_KEY = "secret-key";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsBlankId(String id) {
        assertThatThrownBy(() -> new Provider(id, BASE_URL, API_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsBlankBaseUrl(String baseUrl) {
        assertThatThrownBy(() -> new Provider("provider-1", baseUrl, API_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsBlankApiKey(String apiKey) {
        assertThatThrownBy(() -> new Provider("provider-1", BASE_URL, apiKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apiKey");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://provider.example",
            "file:///tmp/providers",
            "provider.example",
            "https://"
    })
    void rejectsNonHttpOrRelativeBaseUrl(String baseUrl) {
        assertThatThrownBy(() -> new Provider("provider-1", baseUrl, API_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl");
    }

    @Test
    void acceptsValidProvider() {
        Provider provider = new Provider("provider-1", BASE_URL, API_KEY);

        assertThat(provider.id()).isEqualTo("provider-1");
        assertThat(provider.baseUrl()).isEqualTo(BASE_URL);
        assertThat(provider.apiKey()).isEqualTo(API_KEY);
    }

    @Test
    void doesNotExposeApiKeyInToString() {
        Provider provider = new Provider("provider-1", BASE_URL, API_KEY);

        assertThat(provider.toString()).doesNotContain(API_KEY);
    }
}
