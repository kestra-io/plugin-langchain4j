package io.kestra.plugin.langchain4j.openai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.AbstractJSONStructuredExtraction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "OpenAI JSON Structured Extraction Task",
    description = "Generates JSON structured extraction using OpenAI models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Structured Extraction Example",
            code = {
                "fields: [\"name\", \"date\"]",
                "prompt: \"Hello, my name is John\"",
                "model: \"gpt-4o-mini\""
            }
        )
    }
)
public class JSONStructuredExtraction extends AbstractJSONStructuredExtraction {

    @Schema(
        title = "OpenAI Model Name",
        description = "The OpenAI model to use"
    )
    @NotNull
    private Property<OpenAiChatModelName> modelName;

    @Schema(
        title = "API Key",
        description = "API key for the language model"
    )
    @NotNull
    protected Property<String> apiKey;

    @Override
    protected ChatLanguageModel createModel(RunContext runContext) throws Exception {
        OpenAiChatModelName renderedModelName = runContext.render(modelName).as(OpenAiChatModelName.class).orElseThrow();
        String renderedApiKey = runContext.render(apiKey).as(String.class)
            .orElseThrow();
        return OpenAiChatModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderedModelName)
            .logRequests(true)
            .logResponses(true)
            .build();
    }
}
