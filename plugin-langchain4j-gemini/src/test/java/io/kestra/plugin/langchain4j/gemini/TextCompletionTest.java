package io.kestra.plugin.langchain4j.gemini;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.gemini.enums.GeminiModel;
import io.micronaut.context.annotation.Value;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for OpenAITextCompletion
 */
@KestraTest
public class TextCompletionTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    @Value("${kestra.gemini.apikey}")
    private String apikeyTest;

    @Test
    void run() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is the capital of France?",
            "apikey", apikeyTest,
            "modelName", GeminiModel.GEMINI_1_5_FLASH
        ));

        TextCompletion task = TextCompletion.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .apikey(new Property<>("{{ apikey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .build();

        // WHEN
        TextCompletion.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getCompletion().toLowerCase().contains("paris"), is(Boolean.TRUE));
    }
}
