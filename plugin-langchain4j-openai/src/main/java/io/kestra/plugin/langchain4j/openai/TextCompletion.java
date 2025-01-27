package io.kestra.plugin.langchain4j.openai;


import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.AbstractTextCompletion;
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
    title = "OpenAI Text Completion Task",
    description = "Generates text completion using OpenAI models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Text Completion Example",
            code = {
                "prompt: \"What is the capital of France?\"",
                "apiKey: \"demo\"",
                "modelName: \"gpt-4\""
            }
        )
    }
)
public class TextCompletion extends AbstractTextCompletion {
    @Schema(
        title = "OpenAI Model",
        description = "OpenAI model name"
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
    protected ChatLanguageModel createModel(RunContext runContext) throws IllegalVariableEvaluationException {
        OpenAiChatModelName renderedModelName = runContext.render(modelName).as(OpenAiChatModelName.class)
            .orElseThrow();

        String renderedApiKey = runContext.render(apiKey).as(String.class)
            .orElseThrow();

        return OpenAiChatModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderedModelName)
            .build();
    }
}