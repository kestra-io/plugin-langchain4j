package io.kestra.plugin.langchain4j.ollama;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.ollama.enums.EOllamaModel;
import io.kestra.plugin.langchain4j.ollama.ollama.OllamaContainerTest;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


@KestraTest
class TextCompletionTest extends OllamaContainerTest {


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

        TextCompletion task = TextCompletion.builder()
            .ollamaModelName(new Property<>("{{ ollamaModelName }}"))
            .prompt(new Property<>("{{ prompt }}"))
            .ollamaEndpoint(new Property<>("{{ ollamaEndpoint }}"))
            .build();

        // WHEN
        TextCompletion.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getCompletion(), notNullValue());
        assertThat(runOutput.getCompletion().toLowerCase().contains("paris"), is(Boolean.TRUE));
    }
}