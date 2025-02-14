package io.kestra.plugin.langchain4j;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.dto.text.Provider;
import io.kestra.plugin.langchain4j.dto.text.ProviderConfig;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.kestra.plugin.langchain4j.dto.text.Provider.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
class ClassificationTest extends ContainerTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    @Value("${kestra.gemini.apikey}")
    private String apikeyTest;

    @Test
    void testClassificationGemini() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Is 'This is a joke' a good joke?",
            "classes", List.of("true", "false"),
            "apiKey", apikeyTest,
            "modelName", "gemini-1.5-flash",
            "modelProvider", GOOGLE_GEMINI

        ));

        Classification task = Classification.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .classes(new Property<>("{{ classes }}"))
            .provider(ProviderConfig.builder()
                .type(new Property<>("{{ modelProvider }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .build();

        // WHEN
        Classification.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getClassification(), notNullValue());
        assertThat(List.of("true", "false").contains(runOutput.getClassification().toLowerCase()), is(Boolean.TRUE));
    }


    @Test
    void testClassificationOllama() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Is 'This is a joke' a good joke?",
            "classes", List.of("true", "false"),
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "modelProvider", OLLAMA
        ));

        Classification task = Classification.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .classes(new Property<>("{{ classes }}"))
            .provider(ProviderConfig.builder()
                .type(new Property<>("{{ modelProvider }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .endPoint(new Property<>("{{ endpoint }}"))
                .build()
            )
            .build();

        // WHEN
        Classification.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getClassification(), notNullValue());
        assertThat(List.of("true", "false").contains(runOutput.getClassification().toLowerCase()), is(Boolean.TRUE));
    }


    @Test
    void testClassificationOpenAI() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Is 'This is a joke' a good joke?",
            "classes", List.of("true", "false"),
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "modelProvider", OPENAI
        ));

        Classification task = Classification.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .classes(new Property<>("{{ classes }}"))
            .provider(ProviderConfig.builder()
                .type(new Property<>("{{ modelProvider }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
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
