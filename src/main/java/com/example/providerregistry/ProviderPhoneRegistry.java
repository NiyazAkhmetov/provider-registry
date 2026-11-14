package com.example.providerregistry;

import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Реестр HTTP-клиентов поставщиков.
 *
 * <p>Набор поставщиков заменяется целиком методом {@link #setProviders(List)}.
 * Замена атомарна: уже выполняющиеся запросы продолжают работать со старым набором,
 * новые запросы видят новый.</p>
 */
public final class ProviderPhoneRegistry {

    private final RestClient.Builder restClientBuilder;

    private volatile Map<String, ProviderClient> clients = Map.of();

    public ProviderPhoneRegistry(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = Objects.requireNonNull(restClientBuilder, "restClientBuilder");
    }

    /**
     * Полностью заменяет набор поставщиков.
     *
     * @throws NullPointerException если {@code providers} или один из элементов равен {@code null}
     * @throws IllegalArgumentException если повторяются id
     */
    public synchronized void setProviders(List<Provider> providers) {
        Objects.requireNonNull(providers, "providers");

        Map<String, ProviderClient> newClients = new HashMap<>();
        for (Provider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            if (newClients.containsKey(provider.id())) {
                throw new IllegalArgumentException("Duplicate provider id: " + provider.id());
            }
            newClients.put(provider.id(), new ProviderClient(restClientBuilder, provider));
        }

        this.clients = Map.copyOf(newClients);
    }

    /**
     * Проверяет доступность номера телефона у указанного поставщика.
     *
     * @return {@code true}, если поставщик ответил HTTP 200 с телом {@code {"status":"ok"}};
     *         {@code false}, если поставщик ответил HTTP 404
     * @throws IllegalArgumentException если {@code providerId} или {@code phone} пусты
     * @throws UnknownProviderException если поставщик с таким id не зарегистрирован
     * @throws ProviderResponseException если поставщик ответил HTTP 200 с неожиданным телом
     * @throws org.springframework.web.client.RestClientException при прочих HTTP-ошибках
     *         и сетевых ошибках
     */
    public boolean isPhoneAvailable(String providerId, String phone) {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phone must not be blank");
        }

        ProviderClient client = clients.get(providerId);
        if (client == null) {
            throw new UnknownProviderException(providerId);
        }
        return client.isPhoneAvailable(phone);
    }

    /**
     * Идентификаторы текущего набора поставщиков.
     */
    public Set<String> providerIds() {
        return clients.keySet();
    }
}
