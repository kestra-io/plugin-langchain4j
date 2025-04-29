package io.kestra.plugin.langchain4j;

import com.fasterxml.jackson.databind.JsonNode;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.langchain4j.provider.GoogleGemini;
import io.kestra.plugin.langchain4j.provider.Ollama;
import io.kestra.plugin.langchain4j.provider.OpenAI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@KestraTest
class JSONStructuredExtractionTest extends ContainerTest {
    private final String GEMINI_APIKEY = System.getenv("GEMINI_APIKEY");

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_APIKEY", matches = ".*")
    void testJSONStructuredGemini() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Hello, my name is John. I was born on January 1, 2000.",
            "jsonFields", List.of("name", "date"),
            "schemaName", "Person",
            "modelName", "gemini-1.5-flash",
            "apiKey", GEMINI_APIKEY
        ));

        JSONStructuredExtraction task = JSONStructuredExtraction.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .schemaName(new Property<>("{{ schemaName }}"))
            .jsonFields(new Property<>("{{ jsonFields }}"))
            .provider(GoogleGemini.builder()
                .type(GoogleGemini.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .build()
            )
            .build();

        // WHEN
        JSONStructuredExtraction.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getExtractedJson(), notNullValue());

        JsonNode json = JacksonMapper.ofJson().readTree(runOutput.getExtractedJson());
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
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint
        ));

        JSONStructuredExtraction task = JSONStructuredExtraction.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .schemaName(new Property<>("{{ schemaName }}"))
            .jsonFields(new Property<>("{{ jsonFields }}"))
            .provider(Ollama.builder()
                .type(Ollama.class.getName())
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
    @Disabled("demo apikey has quotas")
    void testJSONStructuredOpenAI() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Hello, my name is John. I was born on January 1, 2000.",
            "jsonFields", List.of("name", "date"),
            "schemaName", "Person",
            "modelName", "gpt-4o-mini",
            "apiKey", "demo",
            "baseUrl", "http://langchain4j.dev/demo/openai/v1"
        ));

        JSONStructuredExtraction task = JSONStructuredExtraction.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .schemaName(new Property<>("{{ schemaName }}"))
            .jsonFields(new Property<>("{{ jsonFields }}"))
            .provider(OpenAI.builder()
                .type(OpenAI.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .baseUrl(new Property<>("{{ baseUrl}}"))
                .build()
            )
            .build();

        // WHEN
        JSONStructuredExtraction.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getExtractedJson(), notNullValue());
        JsonNode json = JacksonMapper.ofJson().readTree(runOutput.getExtractedJson());
        assertThat(json.has("name"), is(true));
        assertThat(json.has("date"), is(true));
    }
}
