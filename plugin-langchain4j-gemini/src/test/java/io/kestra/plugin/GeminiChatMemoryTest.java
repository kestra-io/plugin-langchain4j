package io.kestra.plugin;


import dev.langchain4j.data.message.ChatMessage;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.dto.ChatMessageDTO;
import io.kestra.plugin.enums.EChatType;
import io.kestra.plugin.enums.EGeminiModel;
import io.micronaut.context.annotation.Value;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for GoogleGeminiChatModel
 */
@KestraTest
class GeminiChatMemoryTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    @Value("${kestra.gemini.apikey}")
    private String apikeyTest;

    @Test
    void run() throws Exception {
        // GIVEN: First prompt
        RunContext runContext = runContextFactory.of(Map.of(
            "apikey", apikeyTest,
            "modelName", EGeminiModel.GEMINI_1_5_FLASH,
            "chatMessagesInput", List.of(ChatMessageDTO.builder().type(EChatType.USER)
                .content("Hello, my name is John")
                .build())
        ));

        GeminiChatMemory firstTask = GeminiChatMemory.builder()
            .apikey(new Property<>("{{ apikey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .chatMessagesInput(new Property<>("{{ chatMessagesInput }}"))
            .build();

        // WHEN: Run the first task
        GeminiChatMemory.Output firstOutput = firstTask.run(runContext);

        // THEN: Validate the first response
        assertThat(firstOutput.getOutputMessages().size(), is(2)); // User and AI response
        List<ChatMessageDTO> updatedMessages = firstOutput.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatMessageDTO.builder()
            .type(EChatType.USER)
            .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "apikey", apikeyTest,
            "modelName", EGeminiModel.GEMINI_1_5_FLASH,
            "chatMessagesInput", updatedMessages // Pass updated messages
        ));

        GeminiChatMemory secondTask = GeminiChatMemory.builder()
            .apikey(new Property<>("{{ apikey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .chatMessagesInput(new Property<>("{{ chatMessagesInput }}"))
            .build();

        // WHEN: Run the second task
        GeminiChatMemory.Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), containsString("John"));
        assertThat(secondOutput.getOutputMessages().size(), is(4)); // Two user and two AI responses
    }
}
