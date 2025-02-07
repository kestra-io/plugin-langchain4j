package io.kestra.plugin.langchain4j.gemini;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.AbstractChatCompletion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Gemini Chat Memory Task",
    description = "Handles chat interactions with memory using Gemini models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Chat Memory Example",
            full = true,
            code = {
                """
                id: gemini_chat_completion
                namespace: company.team
                task:
                    id: chat_completion
                    apiKey: your_gemini_api_key
                    modelName: gemini-1.5-flash
                    Messages:
                      - type: USER
                        content: What is the capital of France?
                      - type: AI
                        content: The capital of France is Paris.
                      - type: USER
                        content: Can you also tell me its population?
                """
            }
        )
    }
)
public class ChatCompletion extends AbstractChatCompletion {

    @Schema(
        title = "Gemini Model Name",
        description = "Name of the Gemini model to use"
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
    protected ChatLanguageModel createModel(RunContext runContext) throws IllegalVariableEvaluationException {
        String renderedApiKey = runContext.render(apiKey).as(String.class).orElseThrow();

        return GoogleAiGeminiChatModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderModelName(runContext))
            .logRequestsAndResponses(true)
            .build();
    }

    private String renderModelName (RunContext runContext) throws IllegalVariableEvaluationException {
        return runContext.render(modelName).as(String.class).orElseThrow();
    }
}

