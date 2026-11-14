package com.example.providerregistry;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Загружает список поставщиков из JSON-файла.
 *
 * <p>Ожидается JSON-массив объектов {@link Provider}:</p>
 * <pre>{@code
 * [
 *   {"id": "alpha", "baseUrl": "https://alpha.example", "apiKey": "alpha-key"}
 * ]
 * }</pre>
 */
public final class ProviderJsonLoader {

    private final ObjectMapper objectMapper;

    public ProviderJsonLoader(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Читает JSON-массив поставщиков из файла.
     *
     * @throws IllegalArgumentException если файл содержит {@code null} вместо массива
     *         или {@code null} среди элементов
     */
    public List<Provider> load(Path file) throws IOException {
        Objects.requireNonNull(file, "file");

        try (InputStream input = Files.newInputStream(file)) {
            Provider[] providers = objectMapper.readValue(input, Provider[].class);
            if (providers == null) {
                throw new IllegalArgumentException(
                        "Providers file must contain a JSON array, but contains null: " + file);
            }
            return Arrays.stream(providers)
                    .map(provider -> requireProvider(provider, file))
                    .toList();
        }
    }

    private static Provider requireProvider(Provider provider, Path file) {
        if (provider == null) {
            throw new IllegalArgumentException("Providers file contains a null provider: " + file);
        }
        return provider;
    }
}
