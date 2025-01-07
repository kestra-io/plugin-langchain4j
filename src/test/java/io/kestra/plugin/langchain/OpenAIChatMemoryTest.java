package io.kestra.plugin.langchain;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.kestra.plugin.langchain.utils.ConstantTest.OPENAI_DEMO_APIKEY;
import static io.kestra.plugin.langchain.utils.ConstantTest.OPENAI_TEXT_MINI_MODEL;

/**
 * Unit test for OpenAIChatMemory
 */
@KestraTest
class OpenAIChatMemoryTest {

    @Inject
    private RunContextFactory runContextFactory;
    @Test
    void run() throws Exception {
        // Initialize task instance
        OpenAIChatMemory task = OpenAIChatMemory.builder()
            .userMessage(new Property<>("{{ userMessage }}"))
            .apiKey(new Property<>("{{ apiKey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .maxTokens(new Property<>("{{ maxTokens }}"))
            .build();

        // GIVEN: run with first user message
        RunContext runContext = runContextFactory.of(Map.of(
            "userMessage", "Hello, my name is John",
            "apiKey", OPENAI_DEMO_APIKEY,
            "modelName", OPENAI_TEXT_MINI_MODEL,
            "maxTokens", 500
        ));
        // WHEN
        OpenAIChatMemory.Output step1Output = task.run(runContext);

        // THEN
        Assertions.assertTrue(step1Output.getAiResponse().toLowerCase().contains("John".toLowerCase()));

        // GIVEN: second step
        runContext = runContextFactory.of(Map.of(
            "userMessage", "What is my name?",
            "apiKey", OPENAI_DEMO_APIKEY,
            "modelName", OPENAI_TEXT_MINI_MODEL,
            "maxTokens", 500
        ));
        // WHEN: run same context with different prompt user message
        OpenAIChatMemory.Output step2Output = task.run(runContext);

        // THEN : verify 2end output
        Assertions.assertTrue(step2Output.getAiResponse().toLowerCase().contains("Your name is John".toLowerCase()));

    }


}
