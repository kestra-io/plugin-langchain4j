package io.kestra.plugin.langchain;

import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain.OpenAIChatMemory.Output;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;

import static io.kestra.plugin.langchain.utils.ConstantTest.OPENAI_DEMO_APIKEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for OpenAIChatMemory
 */
@KestraTest
class OpenAIChatMemoryTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // GIVEN: First prompt
        RunContext runContext = runContextFactory.of(Map.of(
            "userMessage", "Hello, my name is John",
            "apiKey", OPENAI_DEMO_APIKEY,
            "modelName", OpenAiChatModelName.GPT_4_O_MINI.name(),
            "maxTokens", 300
        ));

        OpenAIChatMemory firstTask = OpenAIChatMemory.builder()
            .userMessage(new Property<>("{{ userMessage }}"))
            .apiKey(new Property<>("{{ apiKey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .maxTokens(new Property<>("{{ maxTokens }}"))
            .build();

        // WHEN: Run the first task
        Output firstOutput = firstTask.run(runContext);

        // THEN: Validate the first response
        assertThat(firstOutput.getChatMemoryId(), is(notNullValue()));

        UUID chatMemoryId = firstOutput.getChatMemoryId();

        // GIVEN: Second prompt using the same memory ID
        runContext = runContextFactory.of(Map.of(
            "userMessage", "What's my name?",
            "chatMemoryId", chatMemoryId.toString(),
            "apiKey", OPENAI_DEMO_APIKEY,
            "modelName", OpenAiChatModelName.GPT_4_O_MINI.name(),
            "maxTokens", 300
        ));

        OpenAIChatMemory secondTask = OpenAIChatMemory.builder()
            .userMessage(new Property<>("{{ userMessage }}"))
            .chatMemoryId(new Property<>("{{ chatMemoryId }}"))
            .apiKey(new Property<>("{{ apiKey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .maxTokens(new Property<>("{{ maxTokens }}"))
            .build();

        // WHEN: Run the second task
        Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), is(notNullValue()));
        assertThat(secondOutput.getAiResponse().toLowerCase().contains("john"), is(Boolean.TRUE));
    }
}
