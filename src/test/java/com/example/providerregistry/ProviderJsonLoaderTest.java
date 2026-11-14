package com.example.providerregistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderJsonLoaderTest {

    @TempDir
    Path tempDir;

    private final ProviderJsonLoader loader =
            new ProviderJsonLoader(JsonMapper.builder().build());

    @Test
    void loadsProvidersFromJsonFile() throws Exception {
        Path file = tempDir.resolve("providers.json");
        Files.writeString(file, """
                [
                  {"id": "alpha", "baseUrl": "https://alpha.example", "apiKey": "alpha-key"},
                  {"id": "beta", "baseUrl": "https://beta.example/", "apiKey": "beta-key"}
                ]
                """);

        List<Provider> providers = loader.load(file);

        assertThat(providers).containsExactly(
                new Provider("alpha", "https://alpha.example", "alpha-key"),
                new Provider("beta", "https://beta.example/", "beta-key"));
    }

    @Test
    void rejectsJsonNullInsteadOfArray() throws Exception {
        Path file = tempDir.resolve("providers.json");
        Files.writeString(file, "null");

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON array");
    }

    @Test
    void rejectsNullProviderInArray() throws Exception {
        Path file = tempDir.resolve("providers.json");
        Files.writeString(file, "[null]");

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null provider");
    }

    @Test
    void ignoresUnknownProviderFields() throws Exception {
        Path file = tempDir.resolve("providers.json");
        Files.writeString(file, """
                [
                  {
                    "id": "alpha",
                    "baseUrl": "https://alpha.example",
                    "apiKey": "alpha-key",
                    "comment": "field added by a newer config version"
                  }
                ]
                """);

        List<Provider> providers = loader.load(file);

        assertThat(providers).containsExactly(
                new Provider("alpha", "https://alpha.example", "alpha-key"));
    }

    @Test
    void failsWhenFileDoesNotExist() {
        Path file = tempDir.resolve("missing.json");

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(IOException.class);
    }

    @Test
    void failsOnMalformedJsonFile() throws Exception {
        Path file = tempDir.resolve("providers.json");
        Files.writeString(file, "{not json");

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(JacksonException.class);
    }
}
