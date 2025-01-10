package io.kestra.plugin;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.enums.EGeminiModel;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for OpenAITextCompletion
 */
@KestraTest
public class GeminiTextCompletionTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is the capital of France?",
            "apikey", "AIzaSyAYcZI6PrLZu3Wx03X3zGocPo5x_DXb2PA",
            "modelName", EGeminiModel.GEMINI_1_5_FLASH
        ));

        GeminiTextCompletion task = GeminiTextCompletion.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .apikey(new Property<>("{{ apikey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .build();

        // WHEN
        GeminiTextCompletion.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getCompletion().toLowerCase().contains("paris"), is(Boolean.TRUE));
    }
}
