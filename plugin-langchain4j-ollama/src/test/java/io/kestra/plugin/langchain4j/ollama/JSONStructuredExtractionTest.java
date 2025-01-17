package io.kestra.plugin.langchain4j.ollama;


import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.ollama.enums.EOllamaModel;
import io.kestra.plugin.langchain4j.ollama.ollama.OllamaContainerTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class JSONStructuredExtractionTest extends OllamaContainerTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN
        List<String> jsonFields = List.of("location", "temperature");

        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "In Paris France, it's 20 degrees Celsius.",
            "ollamaEndpoint", ollamaEndpoint,
            "ollamaModelName", EOllamaModel.TINY_DOLPHIN,
            "jsonFields", jsonFields,
            "schemaName", "WeatherInfo"
        ));

        JSONStructuredExtraction task = JSONStructuredExtraction.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .ollamaEndpoint(new Property<>("{{ ollamaEndpoint }}"))
            .ollamaModelName(new Property<>("{{ ollamaModelName }}"))
            .jsonFields(new Property<>("{{ jsonFields }}"))
            .schemaName(new Property<>("{{ schemaName }}"))
            .build();

        // WHEN
        JSONStructuredExtraction.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput, notNullValue());
        assertThat(runOutput.getResult(), notNullValue());

        // Parse and validate JSON response
        JSONObject json = new JSONObject(runOutput.getResult());
        assertThat(json.has("location"), is(true));
        assertThat(json.has("temperature"), is(true));
    }
}

