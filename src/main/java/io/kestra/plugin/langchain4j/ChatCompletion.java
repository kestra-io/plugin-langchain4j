package io.kestra.plugin.langchain4j;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.ChatConfiguration;
import io.kestra.plugin.langchain4j.domain.ModelProvider;
import io.kestra.plugin.langchain4j.domain.ToolProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.List;

import static io.kestra.core.utils.Rethrow.throwFunction;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Complete chats with AI models.",
    description = "Handles chat interactions using AI models (OpenAI, Ollama, Gemini, Anthropic, MistralAI, Deepseek).")
@Plugin(
    examples = {
        @Example(
            title = "Chat completion with Google Gemini",
            full = true,
            code = {
                """
                id: chat_completion
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion
                    type: io.kestra.core.plugin.langchain4j.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                        apiKey: "{{secret('GOOGLE_API_KEY')}}"
                        modelName: gemini-2.0-flash
                    messages:
                      - type: SYSTEM
                        content: You are a helpful assistant, answer concisely, avoid overly casual language or unnecessary verbosity.
                      - type: USER
                        content: "{{inputs.prompt}}"
                """
            }
        ),
        @Example(
            title = "Chat Completion with Google Gemini and a WebSearch tool",
            full = true,
            code = {
                """
                id: chat_completion_with_tools
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion_with_tools
                    type: io.kestra.core.plugin.langchain4j.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                        apiKey: "{{secret('GOOGLE_API_KEY')}}"
                        modelName: gemini-2.0-flash
                    messages:
                      - type: SYSTEM
                        content: You are a helpful assistant, answer concisely, avoid overly casual language or unnecessary verbosity.
                      - type: USER
                        content: "{{inputs.prompt}}
                    tools:
                    - type: io.kestra.plugin.langchain4j.tool.GoogleCustomWebSearch
                      apiKey: "{{ secret('GOOGLE_SEARCH_API_KEY') }}"
                      csi: "{{ secret('GOOGLE_SEARCH_CSI') }}"
                """
            }
        ),
    },
    beta = true
)
public class ChatCompletion extends Task implements RunnableTask<ChatCompletion.Output> {

    @Schema(title = "Chat Messages", description = "The list of chat messages for the current conversation")
    @NotNull
    protected Property<List<ChatMessage>> messages;

    @Schema(title = "Language Model Provider")
    @NotNull
    @PluginProperty
    private ModelProvider provider;

    @Schema(title = "Chat configuration")
    @NotNull
    @PluginProperty
    @Builder.Default
    private ChatConfiguration configuration = ChatConfiguration.empty();

    @Schema(title = "Tools that the LLM may use to augment its response")
    @Nullable
    private Property<List<ToolProvider>> tools;

    @Override
    public ChatCompletion.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render existing messages
        List<ChatMessage> renderedChatMessagesInput = runContext.render(messages).asList(ChatMessage.class);
        List<dev.langchain4j.data.message.ChatMessage> chatMessages = convertMessages(renderedChatMessagesInput);

        // Get the appropriate model from the factory
        ChatModel model = this.provider.chatModel(runContext, configuration);

        List<ToolProvider> toolProviders = runContext.render(tools).asList(ToolProvider.class);
        try {
            // Generate AI response
            Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(buildTools(runContext, toolProviders))
                .build();
            Response<AiMessage> aiResponse = assistant.chat(chatMessages);
            logger.debug("AI Response: {}", aiResponse.content());

            // Return updated messages
            return Output.builder()
                .aiResponse(aiResponse.content().text())
                .tokenUsage(aiResponse.tokenUsage())
                .finishReason(aiResponse.finishReason())
                .build();
        } finally {
            toolProviders.forEach(tool -> tool.close(runContext));
        }
    }

    interface Assistant {
        Response<AiMessage> chat(List<dev.langchain4j.data.message.ChatMessage> chatMessages);
    }

    private List<ToolSpecification> buildTools(RunContext runContext, List<ToolProvider> toolProviders) throws IllegalVariableEvaluationException {
        return toolProviders.stream()
            .flatMap(throwFunction(provider -> provider.tool(runContext).stream()))
            .toList();
    }

    private List<dev.langchain4j.data.message.ChatMessage> convertMessages(List<ChatCompletion.ChatMessage> messages) {
        return messages.stream()
            .map(dto -> switch (dto.type()) {
                case SYSTEM -> SystemMessage.systemMessage(dto.content());
                case AI ->  AiMessage.aiMessage(dto.content());
                case USER ->  UserMessage.userMessage(dto.content());
            })
            .toList();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "AI Response", description = "The generated response from the AI")
        private final String aiResponse;

        @Schema(title = "Token usage")
        private TokenUsage tokenUsage;

        @Schema(title = "Finish reason")
        private FinishReason finishReason;
    }

    @Builder
    public record ChatMessage(ChatMessageType type, String content) {}

    public enum ChatMessageType { SYSTEM, AI, USER }
}
