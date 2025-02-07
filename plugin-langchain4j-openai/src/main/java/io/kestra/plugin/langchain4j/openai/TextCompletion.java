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
            full = true,
            code = {
                """
                id: openai_text_completion
                namespace: company.team

                task:
                    id: text_completion
                    prompt: What is the capital of France?
                    apiKey: your_openai_api_key
                    modelName: gpt-4o-mini
                """
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
    private Property<String> modelName;

    @Schema(
        title = "API Key",
        description = "API key for the language model"
    )
    @NotNull
    protected Property<String> apiKey;

    @Override
    protected ChatLanguageModel createModel(RunContext runContext) throws IllegalVariableEvaluationException {
        String renderedModelName = runContext.render(modelName).as(String.class)
            .orElseThrow();

        String renderedApiKey = runContext.render(apiKey).as(String.class)
            .orElseThrow();

        return OpenAiChatModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderedModelName)
            .build();
    }
}