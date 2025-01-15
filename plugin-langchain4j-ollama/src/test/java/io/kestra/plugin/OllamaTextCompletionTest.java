package io.kestra.plugin;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.enums.EOllamaModel;
import io.kestra.plugin.ollama.OllamaContainerTest;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


@KestraTest
class OllamaTextCompletionTest extends OllamaContainerTest {


    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is the capital of France?",
            "ollamaModelName", EOllamaModel.TINY_DOLPHIN,
            "ollamaEndpoint", ollamaEndpoint
        ));

        OllamaTextCompletion task = OllamaTextCompletion.builder()
            .ollamaModelName(new Property<>("{{ ollamaModelName }}"))
            .prompt(new Property<>("{{ prompt }}"))
            .ollamaEndpoint(new Property<>("{{ ollamaEndpoint }}"))
            .build();

        // WHEN
        OllamaTextCompletion.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getCompletion(), notNullValue());
        assertThat(runOutput.getCompletion().toLowerCase().contains("paris"), is(Boolean.TRUE));
    }
}