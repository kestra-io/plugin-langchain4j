package io.kestra.plugin.langchain4j;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.dto.text.ChatMessage;
import io.kestra.plugin.langchain4j.dto.text.ChatType;
import io.kestra.plugin.langchain4j.dto.text.Provider;
import io.kestra.plugin.langchain4j.dto.text.ProviderConfig;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@KestraTest
class ChatCompletionTest extends ContainerTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    @Value("${kestra.gemini.apikey}")
    private String geminiApikeyTest;

    /**
     * Test Chat Completion using OpenAI.
     */
    @Test
    void testChatCompletionOpenAI() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "messages", List.of(
                ChatMessage.builder().type(ChatType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.OPENAI )
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .build();

        ChatCompletion.Output output = task.run(runContext);

        assertThat(output.getOutputMessages().size(), is(2)); // User and AI response
        List<ChatMessage> updatedMessages = output.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatMessage.builder()
            .type(ChatType.USER)
            .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "messages", updatedMessages
        ));

        ChatCompletion secondTask = ChatCompletion.builder()
            .provider(ProviderConfig.builder()
                .type(Provider.OPENAI)
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .messages(new Property<>("{{ messages }}"))
            .build();

        // WHEN: Run the second task
        ChatCompletion.Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), containsString("John"));
        assertThat(secondOutput.getOutputMessages().size(), is(4));
    }

    /**
     * Test Chat Completion using Gemini.
     */
    @Test
    void testChatCompletionGemini() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "apiKey", geminiApikeyTest,
            "modelName", "gemini-1.5-flash",
            "messages", List.of(
               ChatMessage.builder().type(ChatType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.GOOGLE_GEMINI)
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .build();

        ChatCompletion.Output output = task.run(runContext);

        assertThat(output.getOutputMessages().size(), is(2)); // User and AI response
        List<ChatMessage> updatedMessages = output.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatMessage.builder()
            .type(ChatType.USER)
            .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "apiKey", geminiApikeyTest,
            "modelName", "gemini-1.5-flash",
            "messages", updatedMessages
        ));

        ChatCompletion secondTask = ChatCompletion.builder()
            .provider(ProviderConfig.builder()
                .type(Provider.GOOGLE_GEMINI)
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .messages(new Property<>("{{ messages }}"))
            .build();

        // WHEN: Run the second task
        ChatCompletion.Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), containsString("John"));
        assertThat(secondOutput.getOutputMessages().size(), is(4));
    }

    /**
     * Test Chat Completion using Ollama.
     */
    @Test
    void testChatCompletionOllama() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "ollamaEndpoint", ollamaEndpoint,
            "messages", List.of(
                ChatMessage.builder().type(ChatType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(ProviderConfig.builder()
                .type(Provider.OLLAMA)
                .modelName(new Property<>("{{ modelName }}"))
                .endpoint(new Property<>("{{ ollamaEndpoint }}"))
                .build()
            )
            .build();

        ChatCompletion.Output output = task.run(runContext);

        assertThat(output.getOutputMessages().size(), is(2)); // User and AI response
        List<ChatMessage> updatedMessages = output.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatMessage.builder()
            .type(ChatType.USER)
            .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "ollamaEndpoint", ollamaEndpoint,
            "messages", updatedMessages
        ));

        ChatCompletion secondTask = ChatCompletion.builder()
            .provider(ProviderConfig.builder()
                .type(Provider.OLLAMA)
                .modelName(new Property<>("{{ modelName }}"))
                .endpoint(new Property<>("{{ ollamaEndpoint }}"))
                .build()
            )
            .messages(new Property<>("{{ messages }}"))
            .build();

        // WHEN: Run the second task
        ChatCompletion.Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), containsString("John"));
        assertThat(secondOutput.getOutputMessages().size(), is(4));
    }
}
