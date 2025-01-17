package io.kestra.plugin.langchain4j.openai;

import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

/**
 * Unit test for Classification
 */
@KestraTest
class ClassificationTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Is 'This is a joke' a good joke?",
            "apikey", "demo",
            "classes", List.of("true", "false"),
            "openAiChatModelName", OpenAiChatModelName.GPT_4_O_MINI.name()
        ));

        Classification task = Classification.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .apikey(new Property<>("{{ apikey }}"))
            .classes(new Property<>("{{ classes }}"))
            .openAiChatModelName(new Property<>("{{ openAiChatModelName }}"))
            .build();

        // WHEN
        Classification.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getLabel(), is(oneOf("true", "false"))); // Verify that the result is one of the expected classes
    }
}
