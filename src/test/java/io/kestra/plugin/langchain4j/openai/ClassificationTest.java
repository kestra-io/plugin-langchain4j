package io.kestra.plugin.langchain4j.openai;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.Classification;
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
class ClassificationTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Is 'This is a joke' a good joke?",
            "classes", List.of("true", "false"),
            "apiKey", "demo",
            "modelName", "gpt-4o-mini"
        ));

        Classification task = Classification.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .classes(new Property<>("{{ classes }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.OPENAI)
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .build();

        // WHEN
        Classification.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getClassification(), notNullValue());
        assertThat(List.of("true", "false").contains(runOutput.getClassification().toLowerCase()), is(Boolean.TRUE));
    }
}
