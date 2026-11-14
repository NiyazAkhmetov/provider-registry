package com.example.providerregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Тело успешного ответа поставщика.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubscriberResponse(String status) {
}
