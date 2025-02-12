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

import java.util.Map;

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
            "modelName", "gemini-1.5-flash"
        ));

        TextCompletion task = TextCompletion.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.GOOGLE_GEMINI)
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
            "endpoint", ollamaEndpoint
        ));

        TextCompletion task = TextCompletion.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.OLLAMA)
                .modelName(new Property<>("{{ modelName }}"))
                .endpoint(new Property<>("{{ endpoint }}"))
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
            "modelName", "gpt-4o-mini"
        ));

        TextCompletion task = TextCompletion.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.OPENAI)
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
