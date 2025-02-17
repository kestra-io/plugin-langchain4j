package io.kestra.plugin.langchain4j.gemini;


import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.micronaut.context.annotation.Value;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit test for OpenAIJSONStructuredExtraction
 */
@KestraTest
class JSONStructuredExtractionTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    @Value("${kestra.gemini.apikey}")
    private String apikeyTest;

    @Test
    void run() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Hello, my name is John. I was born on January 1, 2000.",
            "jsonFields", List.of("name", "date"),
            "schemaName", "Person",
            "apiKey", apikeyTest,
            "modelName", "gemini-1.5-flash"
        ));

        JSONStructuredExtraction task = JSONStructuredExtraction.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .jsonFields(new Property<>("{{ jsonFields }}"))
            .schemaName(new Property<>("{{ schemaName }}"))
            .apiKey(new Property<>("{{ apiKey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .build();

        // WHEN
        JSONStructuredExtraction.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getResult().contains("\"name\": \"John\""), is(Boolean.TRUE));
        JSONObject json = new JSONObject(runOutput.getResult());
        assertThat(json.has("name"), is(true));
        assertThat(json.has("date"), is(true));
        assertThat(json.getString("name"), equalToIgnoringCase("John"));
    }
}
