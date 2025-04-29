package io.kestra.plugin.langchain4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.ChatConfiguration;
import io.kestra.plugin.langchain4j.domain.ModelProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Complete chats with AI models.", 
    description = "Handles chat interactions using AI models (OpenAI, Ollama, Gemini).")
@Plugin(
    examples = {
        @Example(
            title = "Chat with OpenAI",
            full = true,
            code = {
                """
                id: openai_chat_completion
                namespace: company.team
                task:
                    id: chat_completion
                    type: io.kestra.core.plugin.langchain4j.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.OpenAI
                        apiKey: your_openai_api_key
                        modelName: gpt-4o-mini
                    messages:
                      - type: USER
                        content: Hello, my name is John
                      - type: AI
                        content: Welcome John, how can I assist you today?
                      - type: USER
                        content: I need help with my account
                """
            }
        ),
        @Example(
            title = "Chat Completion with Ollama",
            full = true,
            code = {
                """
                id: ollama_chat_completion
                namespace: company.team
                task:
                    id: chat_completion
                    type: io.kestra.core.plugin.langchain4j.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.Ollama
                        modelName: llama3
                        endpoint: http://localhost:11434
                    messages:
                      - type: USER
                        content: Hello, I need information about your services
                """
            }
        )
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

    @Override
    public ChatCompletion.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render existing messages
        List<ChatMessage> renderedChatMessagesInput = runContext.render(messages).asList(ChatMessage.class);
        List<dev.langchain4j.data.message.ChatMessage> chatMessages = convertMessages(renderedChatMessagesInput);

        // Get the appropriate model from the factory
        ChatLanguageModel model = this.provider.chatLanguageModel(runContext, configuration);

        // Generate AI response
        AiMessage aiResponse = model.chat(chatMessages).aiMessage();
        logger.debug("AI Response: {}", aiResponse.text());

        // Return updated messages
        return Output.builder()
            .aiResponse(aiResponse.text())
            .outputMessages(renderedChatMessagesInput)
            .build();
    }

    public static List<dev.langchain4j.data.message.ChatMessage> convertMessages(List<ChatCompletion.ChatMessage> messages) {
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

        @Schema(title = "Updated Messages", description = "The updated list of messages after the current interaction")
        private final List<ChatMessage> outputMessages;
    }

    @Builder
    public record ChatMessage(ChatMessageType type, String content) {}

    public enum ChatMessageType { SYSTEM, AI, USER }
}
