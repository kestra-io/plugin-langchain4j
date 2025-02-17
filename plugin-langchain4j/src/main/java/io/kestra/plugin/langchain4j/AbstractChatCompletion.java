package io.kestra.plugin.langchain4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.dto.ChatMessage;
import io.kestra.plugin.langchain4j.enums.ChatType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.List;

import static io.kestra.plugin.langchain4j.utils.LLMUtility.convertFromDTOs;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractChatCompletion extends Task implements RunnableTask<AbstractChatCompletion.Output> {

    @Schema(
        title = "Chat Messages",
        description = "The list of chat messages for the current conversation"
    )
    @NotNull
    protected Property<List<ChatMessage>> messages;

    @Override
    public AbstractChatCompletion.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        // Render existing messages
        List<ChatMessage> renderedChatMessagesInput = runContext.render(messages).asList(ChatMessage.class);

        // Initialize ChatMemory
        List<dev.langchain4j.data.message.ChatMessage> chatMessages = convertFromDTOs(renderedChatMessagesInput);

        // Generate AI response
        ChatLanguageModel model = createModel(runContext);
        AiMessage aiResponse = model.generate(chatMessages).content();
        logger.info("AI Response: {}", aiResponse.text());

        // Add AI response to memory
        renderedChatMessagesInput.add(ChatMessage.builder()
                .type(ChatType.AI)
                .content(aiResponse.text())
            .build());

        // Return updated messages
        return Output.builder()
            .aiResponse(aiResponse.text())
            .outputMessages(renderedChatMessagesInput)
            .build();
    }

    protected abstract ChatLanguageModel createModel(RunContext runContext) throws Exception;


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
        private final List<ChatMessage> outputMessages;
    }


}
