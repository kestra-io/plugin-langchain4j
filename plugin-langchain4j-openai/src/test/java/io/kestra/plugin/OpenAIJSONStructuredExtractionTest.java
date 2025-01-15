package io.kestra.plugin;

import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for OpenAIJSONStructuredExtraction
 */
@KestraTest
class OpenAIJSONStructuredExtractionTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Hello, my name is John. I was born on January 1, 2000.",
            "jsonFields", List.of("name", "date"),
            "schemaName", "UserDetails",
            "apikey", "demo",
            "openAiChatModelName", OpenAiChatModelName.GPT_4_O_MINI.name()
        ));

        OpenAIJSONStructuredExtraction task = OpenAIJSONStructuredExtraction.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .jsonFields(new Property<>("{{ jsonFields }}"))
            .schemaName(new Property<>("{{ schemaName }}"))
            .apikey(new Property<>("{{ apikey }}"))
            .openAiChatModelName(new Property<>("{{ openAiChatModelName }}"))
            .build();

        // WHEN
        OpenAIJSONStructuredExtraction.Output runOutput = task.run(runContext);

        // THEN
        JSONObject json = new JSONObject(runOutput.getResult());
        assertThat(json.has("name"), is(true));
        assertThat(json.has("date"), is(true));
        assertThat(json.getString("name"), equalToIgnoringCase("John"));
    }
}
