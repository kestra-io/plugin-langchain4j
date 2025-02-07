package io.kestra.plugin.langchain4j.ollama;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
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
    title = "Ollama Chat Memory Task",
    description = "Handles chat interactions with memory using Ollama models"
)

@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Chat Memory Example",
            full = true,
            code = {
                """
                id: ollama_chat_completion
                namespace: company.team

                task:
                    id: chat_completion
                    modelName: llama3
                    ollamaEndpoint: http://localhost:11434
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
        title = "Ollama Model Name",
        description = "Name of the Ollama model to use"
    )
    @NotNull
    private Property<String> modelName;


    @Schema(
        title = "Ollama Endpoint",
        description = "The base URL for the Ollama API"
    )
    @NotNull
    private Property<String> ollamaEndpoint;

    @Override
    protected ChatLanguageModel createModel(RunContext runContext) throws IllegalVariableEvaluationException {

        String renderedUrl = runContext.render(ollamaEndpoint).as(String.class).orElseThrow();
        String renderedModelName= runContext.render(modelName).as(String.class).orElseThrow();

        return OllamaChatModel.builder()
            .baseUrl(renderedUrl)
            .modelName(renderedModelName)
            .logRequests(true)
            .logResponses(true)
            .build();
    }

}

