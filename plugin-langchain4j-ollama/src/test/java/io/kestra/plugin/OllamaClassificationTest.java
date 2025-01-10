package io.kestra.plugin;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.enums.EOllamaModel;
import io.kestra.plugin.ollama.OllamaContainerTest;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.in;

@KestraTest
class OllamaClassificationTest extends OllamaContainerTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN
        List<String> classes = List.of("Paris", "London", "Berlin");

        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is the capital of France?",
            "ollamaModelName", EOllamaModel.TINY_DOLPHIN,
            "ollamaEndpoint", ollamaEndpoint,
            "classes", classes
        ));

        OllamaClassification task = OllamaClassification.builder()
            .ollamaModelName(new Property<>("{{ ollamaModelName }}"))
            .prompt(new Property<>("{{ prompt }}"))
            .ollamaEndpoint(new Property<>("{{ ollamaEndpoint }}"))
            .classes(new Property<>("{{ classes }}"))
            .build();

        // WHEN
        OllamaClassification.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getLabel(), is(in(classes)));
        assertThat(runOutput.getLabel().equals("Paris"), is(Boolean.TRUE));
    }
}