package com.example.providerregistry;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP-клиент одного поставщика.
 */
final class ProviderClient {

    static final String API_KEY_HEADER = "X-API-Key";
    private static final String SUBSCRIBERS_PATH = "/subscribers/{phone}";
    private static final String OK_STATUS = "ok";

    private final Provider provider;
    private final RestClient restClient;

    ProviderClient(RestClient.Builder restClientBuilder, Provider provider) {
        this.provider = provider;
        // baseUrl(...) creates DefaultUriBuilderFactory with TEMPLATE_AND_VALUES,
        // so path variables are encoded ('+' -> %2B, '/' -> %2F).
        this.restClient = restClientBuilder.clone()
                .baseUrl(withoutTrailingSlashes(provider.baseUrl()))
                .defaultHeader(API_KEY_HEADER, provider.apiKey())
                .build();
    }

    boolean isPhoneAvailable(String phone) {
        ResponseEntity<SubscriberResponse> response;
        try {
            response = restClient.get()
                    .uri(SUBSCRIBERS_PATH, phone)
                    .retrieve()
                    .toEntity(SubscriberResponse.class);
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (RestClientException ex) {
            if (hasCause(ex, HttpMessageConversionException.class)) {
                throw new ProviderResponseException(provider.id(),
                        "response body does not match {\"status\":\"ok\"}", ex);
            }
            throw ex;
        }

        if (response.getStatusCode().value() != HttpStatus.OK.value()) {
            throw new ProviderResponseException(provider.id(), response.getStatusCode().value(),
                    "expected HTTP 200");
        }

        SubscriberResponse body = response.getBody();
        if (body == null || !OK_STATUS.equals(body.status())) {
            throw new ProviderResponseException(provider.id(), response.getStatusCode().value(),
                    "expected body {\"status\":\"ok\"}");
        }

        return true;
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                return false;
            }
            current = cause;
        }
        return false;
    }

    private static String withoutTrailingSlashes(String baseUrl) {
        int end = baseUrl.length();
        while (end > 0 && baseUrl.charAt(end - 1) == '/') {
            end--;
        }
        return baseUrl.substring(0, end);
    }
}
