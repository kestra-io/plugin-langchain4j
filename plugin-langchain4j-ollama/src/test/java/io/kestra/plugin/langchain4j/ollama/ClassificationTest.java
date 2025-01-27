package io.kestra.plugin.langchain4j.ollama;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.ollama.enums.EOllamaModel;
import io.kestra.plugin.langchain4j.ollama.ollama.OllamaContainerTest;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@KestraTest
class ClassificationTest extends OllamaContainerTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN
        List<String> classes = List.of("Paris", "London", "Berlin");

        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is the capital of France?",
            "modelName", EOllamaModel.TINY_DOLPHIN,
            "ollamaEndpoint", ollamaEndpoint,
            "classes", classes
        ));

        Classification task = Classification.builder()
            .modelName(new Property<>("{{ modelName }}"))
            .prompt(new Property<>("{{ prompt }}"))
            .ollamaEndpoint(new Property<>("{{ ollamaEndpoint }}"))
            .classes(new Property<>("{{ classes }}"))
            .build();

        // WHEN
        Classification.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getLabel(), containsString("Paris"));
    }
}