package com.example.providerregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;

/**
 * Описание поставщика: идентификатор, базовый URL и API-ключ.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Provider(String id, String baseUrl, String apiKey) {

    public Provider {
        id = requireText(id, "id");
        baseUrl = requireText(baseUrl, "baseUrl");
        apiKey = requireText(apiKey, "apiKey");
        validateBaseUrl(baseUrl);
    }

    @Override
    public String toString() {
        return "Provider[id=%s, baseUrl=%s, apiKey=***]".formatted(id, baseUrl);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Provider " + field + " must not be blank");
        }
        return value;
    }

    private static void validateBaseUrl(String baseUrl) {
        URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Provider baseUrl is not a valid URI: " + baseUrl, ex);
        }

        boolean httpScheme = "http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme());
        if (!httpScheme || uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "Provider baseUrl must be an absolute http(s) URL: " + baseUrl);
        }
    }
}
