package io.kestra.plugin.langchain4j;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.provider.GoogleGemini;
import io.kestra.plugin.langchain4j.provider.Ollama;
import io.kestra.plugin.langchain4j.provider.OpenAI;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
class ClassificationTest extends ContainerTest {
    private final String GEMINI_APIKEY = System.getenv("GEMINI_APIKEY");

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_APIKEY", matches = ".*")
    void testClassificationGemini() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Is 'This is a joke' a good joke?",
            "classes", List.of("true", "false"),
            "apiKey", GEMINI_APIKEY,
            "modelName", "gemini-1.5-flash"

        ));

        Classification task = Classification.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .classes(new Property<>("{{ classes }}"))
            .provider(GoogleGemini.builder()
                .type(GoogleGemini.class.getName())
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .build();

        // WHEN
        Classification.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getClassification(), notNullValue());
    }


    @Test
    void testClassificationOllama() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Is 'This is a joke' a good joke?",
            "classes", List.of("true", "false"),
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint
        ));

        Classification task = Classification.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .classes(new Property<>("{{ classes }}"))
            .provider(Ollama.builder()
                .type(Ollama.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .endpoint(new Property<>("{{ endpoint }}"))
                .build()
            )
            .build();

        // WHEN
        Classification.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getClassification(), notNullValue());
    }


    @Test
    @Disabled("demo apikey has quotas")
    void testClassificationOpenAI() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Is 'This is a joke' a good joke?",
            "classes", List.of("true", "false"),
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "baseUrl", "http://langchain4j.dev/demo/openai/v1"
        ));

        Classification task = Classification.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .classes(new Property<>("{{ classes }}"))
            .provider(OpenAI.builder()
                .type(OpenAI.class.getName())
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .baseUrl(new Property<>("{{ baseUrl }}"))
                .build()
            )
            .build();

        // WHEN
        Classification.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getClassification(), notNullValue());
        assertThat(List.of("true", "false").contains(runOutput.getClassification().toLowerCase()), is(Boolean.TRUE));
    }
}
