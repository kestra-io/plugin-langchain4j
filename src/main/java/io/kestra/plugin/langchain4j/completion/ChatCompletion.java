package io.kestra.plugin.langchain4j.completion;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
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
            title = "Chat with OpenAI",
            full = true,
            code = {
                """
                id: openai_chat_completion
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion
                    type: io.kestra.core.plugin.langchain4j.completion.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.OpenAI
                        apiKey: your_openai_api_key
                        modelName: gpt-4o-mini
                    messages:
                      - type: SYSTEM
                        content: You are a french AI
                      - type: USER
                        content: "{{inputs.prompt}}"
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

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion
                    type: io.kestra.core.plugin.langchain4j.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.Ollama
                        modelName: llama3
                        endpoint: http://localhost:11434
                    messages:
                      - type: SYSTEM
                        content: You are a french AI
                      - type: USER
                        content: "{{inputs.prompt}}"
                """
            }
        ),
        @Example(
            title = "Chat Completion with Anthropic",
            full = true,
            code = {
                """
                id: anthropic_chat_completion
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion
                    type: io.kestra.core.plugin.langchain4j.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.Anthropic
                        apiKey: your_anthropic_api_key
                        modelName: claude-3-haiku-20240307
                    messages:
                      - type: SYSTEM
                        content: You are a french AI
                      - type: USER
                        content: "{{inputs.prompt}}"
                """
            }
        ),
        @Example(
            title = "Chat Completion with DeepSeek",
            full = true,
            code = {
                """
                id: deepseek_chat_completion
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion
                    type: io.kestra.core.plugin.langchain4j.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.DeepSeek
                        apiKey: your_deepseek_api_key
                        modelName: deepseek-chat
                    messages:
                      - type: SYSTEM
                        content: You are a french AI
                      - type: USER
                        content: "{{inputs.prompt}}"
                """
            }
        ),
        @Example(
            title = "Chat Completion with MistralAI",
            full = true,
            code = {
                """
                id: mistral_chat_completion
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion
                    type: io.kestra.core.plugin.langchain4j.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.MistralAI
                        apiKey: your_mistral_api_key
                        modelName: mistral:7b
                    messages:
                      - type: SYSTEM
                        content: You are a french AI
                      - type: USER
                        content: "{{inputs.prompt}}"
                """
            }
        ),
        @Example(
            title = "Chat Completion with Amazon Bedrock",
            full = true,
            code = {
                """
                id: bedrock_chat_completion
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion
                    type: io.kestra.core.plugin.langchain4j.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.AmazonBedrock
                        accessKeyId: your_access_key_id
                        secretAccessKey: your_secret_access_key
                        modelName: anthropic.claude-3-sonnet-20240229-v1:0
                    configuration:
                        temperature: 0.5
                    messages:
                      - type: SYSTEM
                        content: Are you a french AI (answer with yes or no)?
                      - type: USER
                        content: "{{inputs.prompt}}"
                """
            }
        ),
        @Example(
            title = "Chat Completion with Azure OpenAI",
            full = true,
            code = {
                """
                id: azure_openai_chat_completion
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion
                    type: io.kestra.core.plugin.langchain4j.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.AzureOpenAI
                        apiKey: your_openai_api_key
                        endpoint: https://your-resource.openai.azure.com/
                        modelName: anthropic.claude-3-sonnet-20240229-v1:0
                    configuration:
                        temperature: 0.5
                        topP: 0.5
                        seed: 42
                    messages:
                      - type: SYSTEM
                        content: Are you a french AI (answer with yes or no)?
                      - type: USER
                        content: "{{inputs.prompt}}"
                """
            }
        ),
        @Example(
            title = "Chat Completion with Ollama and a websearch tool",
            full = true,
            code = {
                """
                id: ollama_chat_completion_with_tools
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion_with_tools
                    type: io.kestra.core.plugin.langchain4j.completion.ChatCompletion
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.Ollama
                        modelName: llama3
                        endpoint: http://localhost:11434
                    messages:
                      - type: SYSTEM
                        content: You are a french AI
                      - type: USER
                        content: "{{inputs.prompt}}
                    tools:
                    - type: io.kestra.plugin.langchain4j.tool.GoogleCustomWebSearch
                      apiKey: "{{ secret('GOOGLE_API_KEY') }}"
                      csi: "{{ secret('GOOGLE_CSI_KEY') }}"
                """
            }
        ),
    },
    beta = true,
    aliases = "io.kestra.plugin.langchain4j.ChatCompletion"
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

        // Generate AI response
        Assistant assistant = AiServices.builder(Assistant.class)
            .chatModel(model)
            .tools(buildTools(runContext))
            .build();
        AiMessage aiResponse = assistant.chat(chatMessages);
        logger.debug("AI Response: {}", aiResponse.text());

        // Return updated messages
        return Output.builder()
            .aiResponse(aiResponse.text())
            .outputMessages(renderedChatMessagesInput)
            .build();
    }

    interface Assistant {
        AiMessage chat(List<dev.langchain4j.data.message.ChatMessage> chatMessages);
    }

    private List<Object> buildTools(RunContext runContext) throws IllegalVariableEvaluationException {
        return runContext.render(tools).asList(ToolProvider.class).stream()
            .map(throwFunction(provider -> provider.tool(runContext)))
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

        @Schema(title = "Updated Messages", description = "The updated list of messages after the current interaction")
        private final List<ChatMessage> outputMessages;
    }

    @Builder
    public record ChatMessage(ChatMessageType type, String content) {}

    public enum ChatMessageType { SYSTEM, AI, USER }
}
