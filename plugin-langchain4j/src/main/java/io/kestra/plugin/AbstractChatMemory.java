package io.kestra.plugin;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.dto.ChatMessageDTO;
import io.kestra.plugin.enums.EChatType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.kestra.plugin.utils.MethodUtility.convertFromDTOs;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Abstract Chat Memory Task",
    description = "Abstract class for chat memory tasks supporting different models"
)
public abstract class AbstractChatMemory extends Task implements RunnableTask<AbstractChatMemory.Output> {

    @Schema(
        title = "API Key",
        description = "API key for the language model"
    )
    @NotNull
    protected Property<String> apikey;


    @Schema(
        title = "Chat Messages",
        description = "The list of chat messages for the current conversation"
    )
    protected Property<List<ChatMessageDTO>> chatMessagesInput;

    @Override
    public AbstractChatMemory.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render input properties
        String renderedApiKey = runContext.render(apikey).as(String.class).orElseThrow();

        // Render existing messages
        List<ChatMessageDTO> renderedChatMessagesInput = runContext.render(chatMessagesInput).asList(ChatMessageDTO.class);

        // Initialize ChatMemory
        List<ChatMessage> chatMessages = convertFromDTOs(renderedChatMessagesInput);

        // Generate AI response
        ChatLanguageModel model = createModel(runContext, renderedApiKey);
        AiMessage aiResponse = model.generate(chatMessages).content();
        logger.info("AI Response: {}", aiResponse.text());

        // Add AI response to memory
        renderedChatMessagesInput.add(ChatMessageDTO.builder()
                .type(EChatType.AI)
                .content(aiResponse.text())
            .build());

        // Return updated messages
        return Output.builder()
            .aiResponse(aiResponse.text())
            .outputMessages(renderedChatMessagesInput)
            .build();
    }

    protected abstract ChatLanguageModel createModel(RunContext runContext, String apiKey) throws Exception;


    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "AI Response",
            description = "The generated response from the AI"
        )
        private final String aiResponse;

        @Schema(
            title = "Updated Messages",
            description = "The updated list of messages after the current interaction"
        )
        private final List<ChatMessageDTO> outputMessages;
    }


}
