package io.kestra.plugin.langchain4j.openai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.AbstractChatCompletion;
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
    title = "OpenAI Chat Completion Task",
    description = "Handles chat interactions using OpenAI models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Chat Completion Example",
            full = true,
            code = {
                """
                id: openai_chat_memory
                namespace: company.team
                task:
                    id: chat_memory
                    apikey: your_openai_api_key
                    modelName: gpt-4o-mini
                    Messages:
                      - type: USER
                        content: Hello, my name is John
                      - type: AI
                        content: Welcome John, how can I assist you today?
                      - type: USER
                        content: I need help with my account
                """
            }
        )
    }
)

public class ChatCompletion extends AbstractChatCompletion {

    @Schema(
        title = "OpenAI Model Name",
        description = "OpenAI model name"
    )
    @NotNull
    private Property<String> modelName;

    @Schema(
        title = "API Key",
        description = "API key for the language model"
    )
    @NotNull
    public Property<String> apiKey;

    @Override
    protected ChatLanguageModel createModel(RunContext runContext) throws Exception {
        String renderedApiKey = runContext.render(apiKey).as(String.class).orElseThrow();

        return OpenAiChatModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderModelName(runContext))
            .build();
    }

    private String renderModelName (RunContext runContext) throws IllegalVariableEvaluationException {
        return runContext.render(modelName).as(String.class).orElseThrow();
    }
}
