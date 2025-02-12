package io.kestra.plugin.langchain4j;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.dto.text.Provider;
import io.kestra.plugin.langchain4j.dto.text.ProviderConfig;
import io.micronaut.context.annotation.Value;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@KestraTest
class JSONStructuredExtractionTest extends ContainerTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    @Value("${kestra.gemini.apikey}")
    private String apikeyTest;

    @Test
    void testJSONStructuredGemini() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Hello, my name is John. I was born on January 1, 2000.",
            "jsonFields", List.of("name", "date"),
            "schemaName", "Person",
            "modelName", "gemini-1.5-flash",
            "apiKey", apikeyTest
            ));

        JSONStructuredExtraction task = JSONStructuredExtraction.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .schemaName(new Property<>("{{ schemaName }}"))
            .jsonFields(new Property<>("{{ jsonFields }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.GOOGLE_GEMINI)
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .build()
            )
            .build();

        // WHEN
        JSONStructuredExtraction.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getExtractedJson(), notNullValue());
        JSONObject json = new JSONObject(runOutput.getExtractedJson());
        assertThat(json.has("name"), is(true));
        assertThat(json.has("date"), is(true));
    }

    @Test
    void testJSONStructuredOllama() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Hello, my name is Alice, I live in London",
            "schemaName", "Person",
            "jsonFields", List.of("name", "City"),
            "providerType", "OLLAMA",
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint
        ));

        JSONStructuredExtraction task = JSONStructuredExtraction.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .schemaName(new Property<>("{{ schemaName }}"))
            .jsonFields(new Property<>("{{ jsonFields }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.OLLAMA)
                .modelName(new Property<>("{{ modelName }}"))
                .endpoint(new Property<>("{{ endpoint }}"))
                .build()
            )
            .build();

        // WHEN
        JSONStructuredExtraction.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getExtractedJson(), notNullValue());
        assertThat(runOutput.getExtractedJson().toLowerCase().contains("alice"), is(Boolean.TRUE));
        assertThat(runOutput.getExtractedJson().toLowerCase().contains("london"), is(Boolean.TRUE));
    }
    @Test
    void testJSONStructuredOpaenAI() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Hello, my name is John. I was born on January 1, 2000.",
            "jsonFields", List.of("name", "date"),
            "schemaName", "Person",
            "modelName", "gpt-4o-mini",
            "apiKey", "demo"
        ));

        JSONStructuredExtraction task = JSONStructuredExtraction.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .schemaName(new Property<>("{{ schemaName }}"))
            .jsonFields(new Property<>("{{ jsonFields }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.OPENAI)
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .build()
            )
            .build();

        // WHEN
        JSONStructuredExtraction.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getExtractedJson(), notNullValue());
        JSONObject json = new JSONObject(runOutput.getExtractedJson());
        assertThat(json.has("name"), is(true));
        assertThat(json.has("date"), is(true));
    }
}
