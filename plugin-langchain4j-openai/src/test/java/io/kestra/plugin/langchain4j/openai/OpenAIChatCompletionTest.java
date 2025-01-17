package io.kestra.plugin.langchain4j.openai;

import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.openai.OpenAIChatCompletion;
import io.kestra.plugin.langchain4j.dto.ChatMessageDTO;
import io.kestra.plugin.langchain4j.enums.ChatType;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for OpenAIChatCompletion
 */
@KestraTest
class OpenAIChatCompletionTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN: First prompt
        RunContext runContext = runContextFactory.of(Map.of(
            "apikey", "demo",
            "modelName", OpenAiChatModelName.GPT_4_O_MINI.name(),
            "maxTokens", 1000,
            "chatMessagesInput", List.of(ChatMessageDTO.builder().type(ChatType.USER)
                .content("Hello, my name is John")
                .build())
        ));

        OpenAIChatCompletion firstTask = OpenAIChatCompletion.builder()
            .apikey(new Property<>("{{ apikey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .chatMessagesInput(new Property<>("{{ chatMessagesInput }}"))
            .build();

        // WHEN: Run the first task
        OpenAIChatCompletion.Output firstOutput = firstTask.run(runContext);

        // THEN: Validate the first response
        assertThat(firstOutput.getOutputMessages().size(), is(2)); // User and AI response
        List<ChatMessageDTO> updatedMessages = firstOutput.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatMessageDTO.builder()
                .type(ChatType.USER)
                .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "apikey", "demo",
            "modelName", OpenAiChatModelName.GPT_4_O_MINI.name(),
            "chatMessagesInput", updatedMessages // Pass updated messages
        ));

        OpenAIChatCompletion secondTask = OpenAIChatCompletion.builder()
            .apikey(new Property<>("{{ apikey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .chatMessagesInput(new Property<>("{{ chatMessagesInput }}"))
            .build();

        // WHEN: Run the second task
        OpenAIChatCompletion.Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), containsString("John"));
        assertThat(secondOutput.getOutputMessages().size(), is(4)); // Two user and two AI responses
    }
}
