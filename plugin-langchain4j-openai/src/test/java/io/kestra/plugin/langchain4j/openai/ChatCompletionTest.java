package io.kestra.plugin.langchain4j.openai;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.dto.ChatMessage;
import io.kestra.plugin.langchain4j.enums.ChatType;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for ChatCompletion
 */
@KestraTest
class ChatCompletionTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN: First prompt
        RunContext runContext = runContextFactory.of(Map.of(
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "maxTokens", 1000,
            "messages", List.of(ChatMessage.builder().type(ChatType.USER)
                .content("Hello, my name is John")
                .build())
        ));

        ChatCompletion firstTask = ChatCompletion.builder()
            .apiKey(new Property<>("{{ apiKey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .messages(new Property<>("{{ messages }}"))
            .build();

        // WHEN: Run the first task
        ChatCompletion.Output firstOutput = firstTask.run(runContext);

        // THEN: Validate the first response
        assertThat(firstOutput.getOutputMessages().size(), is(2)); // User and AI response
        List<ChatMessage> updatedMessages = firstOutput.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatMessage.builder()
                .type(ChatType.USER)
                .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "messages", updatedMessages // Pass updated messages
        ));

        ChatCompletion secondTask = ChatCompletion.builder()
            .apiKey(new Property<>("{{ apiKey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .messages(new Property<>("{{ messages }}"))
            .build();

        // WHEN: Run the second task
        ChatCompletion.Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), containsString("John"));
        assertThat(secondOutput.getOutputMessages().size(), is(4)); // Two user and two AI responses
    }
}
