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

import java.util.HashMap;
import java.util.Map;

import static io.kestra.plugin.langchain4j.dto.text.Provider.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
public class TextCompletionTest extends ContainerTest{
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    @Value("${kestra.gemini.apikey}")
    private String apikeyTest;

    @Test
    void testTextCompletionGemini() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is the capital of France?",
            "apiKey", apikeyTest,
            "modelName", "gemini-1.5-flash",
            "modelProvider", GOOGLE_GEMINI
        ));

        TextCompletion task = TextCompletion.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .provider(ProviderConfig.builder()
                .type(new Property<>("{{ modelProvider }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .build();

        // WHEN
        TextCompletion.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getCompletion().toLowerCase().contains("paris"), is(Boolean.TRUE));
    }

    @Test
    void testTextCompletionOllama() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is the capital of France?",
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "modelProvider", OLLAMA

        ));

        TextCompletion task = TextCompletion.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .provider(ProviderConfig.builder()
                .type(new Property<>("{{ modelProvider }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .endPoint(new Property<>("{{ endpoint }}"))
                .build()
            )
            .build();

        // WHEN
        TextCompletion.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getCompletion(), notNullValue());
        assertThat(runOutput.getCompletion().toLowerCase().contains("paris"), is(Boolean.TRUE));
    }

    @Test
    void testTextCompletionOpenAI() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is the capital of France?",
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "modelProvider", OPENAI

        ));

        TextCompletion task = TextCompletion.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .provider(ProviderConfig.builder()
                .type(new Property<>("{{ modelProvider }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .build();

        // WHEN
        TextCompletion.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getCompletion().toLowerCase().contains("paris"), is(Boolean.TRUE));
    }

}
