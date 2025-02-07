package io.kestra.plugin.langchain4j.openai;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for TextCompletion
 */
@KestraTest
public class TextCompletionTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is the capital of France?",
            "apiKey", "demo",
            "modelName", "gpt-4o-mini"
        ));

        TextCompletion task = TextCompletion.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .apiKey(new Property<>("{{ apiKey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .build();

        // WHEN
        TextCompletion.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getCompletion().toLowerCase().contains("paris"), is(Boolean.TRUE));
    }
}
