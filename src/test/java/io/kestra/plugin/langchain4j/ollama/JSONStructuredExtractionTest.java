package io.kestra.plugin.langchain4j.ollama;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.JSONStructuredExtraction;
import io.kestra.plugin.langchain4j.model.Provider;
import io.kestra.plugin.langchain4j.model.ProviderConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
class JSONStructuredExtractionTest extends OllamaContainerTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Hello, my name is Alice, I live in London",
            "schemaName", "Person",
            "jsonFields", List.of("name", "City"),
            "providerType", "OLLAMA",
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint
        ));

        JSONStructuredExtraction task = JSONStructuredExtraction.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .schemaName(new Property<>("{{ schemaName }}"))
            .jsonFields(new Property<>("{{ jsonFields }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.OLLAMA)
                .modelName(new Property<>("{{ modelName }}"))
                .endpoint(new Property<>("{{ endpoint }}"))
                .build()
            )
            .build();

        // WHEN
        JSONStructuredExtraction.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getExtractedJson(), notNullValue());
        assertThat(runOutput.getExtractedJson().toLowerCase().contains("alice"), is(Boolean.TRUE));
        assertThat(runOutput.getExtractedJson().toLowerCase().contains("london"), is(Boolean.TRUE));
    }
}
