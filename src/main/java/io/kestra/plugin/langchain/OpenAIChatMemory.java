package io.kestra.plugin.langchain;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "LangChain4j Chat Memory Task",
    description = "Handles chat interactions with memory using LangChain4j"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Chat Memory Example",
            code = {
                "prompt: \"Hello, my name is John\"",
                "model: \"gpt-4\"",
                "maxTokens: 300"
            }
        )
    }
)
public class OpenAIChatMemory extends Task implements RunnableTask<OpenAIChatMemory.Output> {

    @Schema(
        title = "User message",
        description = "The input message from the user"
    )
    private Property<String> userMessage;

    @Schema(
        title = "API Key",
        description = "OpenAI API key"
    )
    private Property<String> apiKey;

    @Schema(
        title = "OpenAI Model",
        description = "OpenAI model name"
    )
    private Property<String> modelName;

    @Schema(
        title = "Max Tokens",
        description = "Maximum tokens for chat memory"
    )
    private Property<Integer> maxTokens;

    private ChatMemory chatMemory;

    @Override
    public OpenAIChatMemory.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render the input properties
        String renderedUserMessage = runContext.getVariables().get("userMessage").toString();
        String renderedApiKey = runContext.render(apiKey).as(String.class).orElse("demo");
        String renderedModelName = runContext.render(modelName).as(String.class).orElse("gpt-4");
        int renderedMaxTokens = runContext.render(maxTokens).as(Integer.class).orElse(500);

        logger.info("User Message: {}", renderedUserMessage);

        // Initialize ChatMemory if it is null
        if (chatMemory == null) {
            chatMemory = TokenWindowChatMemory.withMaxTokens(renderedMaxTokens, new OpenAiTokenizer(renderedModelName));
        }

        // Add user message to memory
        chatMemory.add(UserMessage.userMessage(renderedUserMessage));

        // Generate AI response
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderedModelName)
            .build();
        logger.info("Messages sent to model: {}", chatMemory.messages());

        AiMessage aiResponse = model.generate(chatMemory.messages()).content();
        logger.info("AI Response: {}", aiResponse.text());

        // Add AI response to memory
        chatMemory.add(aiResponse);

        return Output.builder()
            .aiResponse(aiResponse.text())
            .build();
    }


    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "AI Response",
            description = "The generated response from the AI"
        )
        private final String aiResponse;
    }
}
