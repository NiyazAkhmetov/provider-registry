package com.example.providerregistry;

/**
 * Поставщик с запрошенным идентификатором не зарегистрирован.
 *
 * <p>Наследуется от {@link IllegalArgumentException}: неизвестный id — это ошибка
 * конфигурации или вызова, а не сетевая ошибка и не «номер недоступен».</p>
 */
public class UnknownProviderException extends IllegalArgumentException {

    private final String providerId;

    public UnknownProviderException(String providerId) {
        super("Unknown provider: " + providerId);
        this.providerId = providerId;
    }

    public String getProviderId() {
        return providerId;
    }
}
