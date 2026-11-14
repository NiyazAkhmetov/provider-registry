package com.example.providerregistry;

/**
 * Поставщик ответил успешным HTTP-статусом, но ответ не соответствует контракту.
 */
public class ProviderResponseException extends RuntimeException {

    private final String providerId;
    private final int statusCode;

    public ProviderResponseException(String providerId, int statusCode, String details) {
        this(providerId, statusCode, details, null);
    }

    public ProviderResponseException(String providerId, int statusCode, String details, Throwable cause) {
        super(formatMessage(providerId, statusCode, details), cause);
        this.providerId = providerId;
        this.statusCode = statusCode;
    }

    /**
     * Создаёт исключение, когда HTTP-статус ответа определить не удалось
     * (например, тело не разобралось ещё до проверки статуса).
     */
    public ProviderResponseException(String providerId, String details, Throwable cause) {
        this(providerId, -1, details, cause);
    }

    private static String formatMessage(String providerId, int statusCode, String details) {
        if (statusCode < 0) {
            return "Provider '%s' returned an unexpected response: %s"
                    .formatted(providerId, details);
        }
        return "Provider '%s' returned an unexpected response (HTTP %d): %s"
                .formatted(providerId, statusCode, details);
    }

    public String getProviderId() {
        return providerId;
    }

    /**
     * @return HTTP-статус ответа или {@code -1}, если статус определить не удалось
     */
    public int getStatusCode() {
        return statusCode;
    }
}
