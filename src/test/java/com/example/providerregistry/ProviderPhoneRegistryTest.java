package com.example.providerregistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProviderPhoneRegistryTest {

    private static final String PROVIDER_ID = "provider-1";
    private static final String BASE_URL = "https://provider.example";
    private static final String API_KEY = "secret-key";
    private static final String PHONE = "79001234567";

    private MockRestServiceServer server;
    private ProviderPhoneRegistry registry;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        registry = new ProviderPhoneRegistry(builder);
        registry.setProviders(List.of(new Provider(PROVIDER_ID, BASE_URL, API_KEY)));
    }

    @Test
    void returnsTrueWhenProviderRespondsWithOkStatus() {
        server.expect(requestTo(BASE_URL + "/subscribers/" + PHONE))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(ProviderClient.API_KEY_HEADER, API_KEY))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThat(registry.isPhoneAvailable(PROVIDER_ID, PHONE)).isTrue();
        server.verify();
    }

    @Test
    void throwsProviderResponseExceptionWhenStatusIsNotOk() {
        server.expect(requestTo(BASE_URL + "/subscribers/" + PHONE))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"no\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, PHONE))
                .isInstanceOf(ProviderResponseException.class)
                .hasMessageContaining(PROVIDER_ID)
                .hasMessageContaining("expected body");
        server.verify();
    }

    @Test
    void throwsProviderResponseExceptionWhenBodyIsMissing() {
        server.expect(requestTo(BASE_URL + "/subscribers/" + PHONE))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, PHONE))
                .isInstanceOf(ProviderResponseException.class)
                .hasMessageContaining(PROVIDER_ID)
                .hasMessageContaining("expected body");
        server.verify();
    }

    @Test
    void throwsProviderResponseExceptionWhenJsonBodyIsEmpty() {
        server.expect(requestTo(BASE_URL + "/subscribers/" + PHONE))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        // Пустое тело Spring отдаёт как null, не как ошибку конвертера,
        // поэтому срабатывает проверка body == null.
        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, PHONE))
                .isInstanceOf(ProviderResponseException.class)
                .hasMessageContaining("expected body");
        server.verify();
    }

    @Test
    void throwsProviderResponseExceptionWhenBodyIsMalformedJson() {
        server.expect(requestTo(BASE_URL + "/subscribers/" + PHONE))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{not json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, PHONE))
                .isInstanceOf(ProviderResponseException.class)
                .hasMessageContaining(PROVIDER_ID)
                .hasMessageContaining("response body does not match")
                .hasCauseInstanceOf(RestClientException.class);
        server.verify();
    }

    @Test
    void encodesPlusSignInPhoneNumber() {
        String phone = "+79001234567";

        server.expect(requestTo(BASE_URL + "/subscribers/%2B79001234567"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(ProviderClient.API_KEY_HEADER, API_KEY))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThat(registry.isPhoneAvailable(PROVIDER_ID, phone)).isTrue();
        server.verify();
    }

    @Test
    void returnsFalseWhenProviderRespondsWithNotFound() {
        server.expect(requestTo(BASE_URL + "/subscribers/" + PHONE))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(registry.isPhoneAvailable(PROVIDER_ID, PHONE)).isFalse();
        server.verify();
    }

    @Test
    void propagatesOtherHttpErrorsToTheCaller() {
        server.expect(requestTo(BASE_URL + "/subscribers/" + PHONE))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, PHONE))
                .isInstanceOf(HttpServerErrorException.class);
        server.verify();
    }

    @Test
    void propagatesUnauthorizedToTheCaller() {
        server.expect(requestTo(BASE_URL + "/subscribers/" + PHONE))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, PHONE))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        server.verify();
    }

    @Test
    void doesNotTreatAcceptedWithOkBodyAsSuccess() {
        server.expect(requestTo(BASE_URL + "/subscribers/" + PHONE))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"ok\"}"));

        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, PHONE))
                .isInstanceOf(ProviderResponseException.class)
                .hasMessageContaining("202");
        server.verify();
    }

    @Test
    void propagatesNetworkErrorsToTheCaller() {
        server.expect(requestTo(BASE_URL + "/subscribers/" + PHONE))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, PHONE))
                .isInstanceOf(ResourceAccessException.class);
        server.verify();
    }

    @Test
    void replacesTheWholeProviderSet() {
        registry.setProviders(List.of(
                new Provider("provider-2", "https://provider-2.example", "key-2")));

        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, PHONE))
                .isInstanceOf(UnknownProviderException.class);
        assertThat(registry.providerIds()).containsExactly("provider-2");
    }

    @Test
    void rejectsUnknownProvider() {
        assertThatThrownBy(() -> registry.isPhoneAvailable("missing", PHONE))
                .isInstanceOf(UnknownProviderException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void rejectsDuplicateProviderIds() {
        List<Provider> providers = List.of(
                new Provider("duplicate", "https://one.example", "key-1"),
                new Provider("duplicate", "https://two.example", "key-2"));

        assertThatThrownBy(() -> registry.setProviders(providers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void rejectsNullOrBlankArguments() {
        assertThatThrownBy(() -> registry.isPhoneAvailable(null, PHONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.isPhoneAvailable("", PHONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.isPhoneAvailable("   ", PHONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.isPhoneAvailable(PROVIDER_ID, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullProviderListAndNullElement() {
        assertThatThrownBy(() -> registry.setProviders(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> registry.setProviders(Arrays.asList(
                new Provider("other", "https://other.example", "key"), null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void keepsClientsSeparateForMultipleProviders() {
        registry.setProviders(List.of(
                new Provider("alpha", "https://alpha.example", "alpha-key"),
                new Provider("beta", "https://beta.example", "beta-key")));

        server.expect(requestTo("https://alpha.example/subscribers/123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(ProviderClient.API_KEY_HEADER, "alpha-key"))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://beta.example/subscribers/123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(ProviderClient.API_KEY_HEADER, "beta-key"))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThat(registry.isPhoneAvailable("alpha", "123")).isTrue();
        assertThat(registry.isPhoneAvailable("beta", "123")).isTrue();
        assertThat(registry.providerIds()).containsExactlyInAnyOrder("alpha", "beta");
        server.verify();
    }

    @Test
    void encodesPathSeparatorInPhoneNumber() {
        server.expect(requestTo(BASE_URL + "/subscribers/a%2Fb"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThat(registry.isPhoneAvailable(PROVIDER_ID, "a/b")).isTrue();
        server.verify();
    }
}
