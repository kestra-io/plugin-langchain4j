package io.kestra.plugin;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.enums.EGeminiModel;
import io.micronaut.context.annotation.Value;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

/**
 * Unit test for GeminiClassification
 */
@KestraTest
class GeminiClassificationTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    @Value("${kestra.gemini.apikey}")
    private String apikeyTest;

    @Test
    void run() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Is 'This is a joke' a good joke?",
            "classes", List.of("true", "false"),
            "apikey", apikeyTest,
            "modelName", EGeminiModel.GEMINI_1_5_FLASH
        ));

        GeminiClassification task = GeminiClassification.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .apikey(new Property<>("{{ apikey }}"))
            .classes(new Property<>("{{ classes }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .build();

        // WHEN
        GeminiClassification.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getLabel(), is(oneOf("true", "false"))); // Verify that the result is one of the expected classes
    }
}
