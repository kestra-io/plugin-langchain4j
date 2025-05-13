package io.kestra.plugin.langchain4j;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.mistralai.MistralAiModels;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.provider.AnthropicAI;
import io.kestra.plugin.langchain4j.provider.DeepseekAI;
import io.kestra.plugin.langchain4j.provider.GoogleGemini;
import io.kestra.plugin.langchain4j.provider.MistralAI;
import io.kestra.plugin.langchain4j.provider.Ollama;
import io.kestra.plugin.langchain4j.provider.OpenAI;
import io.micronaut.runtime.Micronaut;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class ChatCompletionTest extends ContainerTest {
    private final String GEMINI_APIKEY = System.getenv("GEMINI_APIKEY");
    private final String ANTHROPIC_APIKEY = System.getenv("ANTHROPIC_API_KEY");
    private final String MISTRAL_APIKEY = System.getenv("MISTRAL_API_KEY");
    private final String DEEPSEEK_APIKEY = System.getenv("DEEPSEEK_API_KEY");

    @Inject
    private RunContextFactory runContextFactory;

    /**
     * Test Chat Completion using OpenAI.
     */
    @Test
    @Disabled("demo apikey has quotas")
    void testChatCompletionOpenAI() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "baseUrl", "http://langchain4j.dev/demo/openai/v1",
            "messages", List.of(
                ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(OpenAI.builder()
                .type(OpenAI.class.getName())
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .baseUrl(new Property<>("{{ baseUrl }}"))
                .build()
            )
            .build();

        ChatCompletion.Output output = task.run(runContext);

        assertThat(output.getAiResponse(), notNullValue());
        List<ChatCompletion.ChatMessage> updatedMessages = output.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatCompletion.ChatMessage.builder()
            .type(ChatCompletion.ChatMessageType.USER)
            .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "baseUrl", "http://langchain4j.dev/demo/openai/v1",
            "messages", updatedMessages
            ));

        ChatCompletion secondTask = ChatCompletion.builder()
            .provider(OpenAI.builder()
                .type(OpenAI.class.getName())
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .baseUrl(new Property<>("{{ baseUrl }}"))
                .build()
            )
            .messages(new Property<>("{{ messages }}"))
            .build();

        // WHEN: Run the second task
        ChatCompletion.Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), containsString("John"));
        assertThat(secondOutput.getOutputMessages().size(), is(2));
    }

    /**
     * Test Chat Completion using Gemini.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_APIKEY", matches = ".*")
    void testChatCompletionGemini() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "apiKey", GEMINI_APIKEY,
            "modelName", "gemini-1.5-flash",
            "messages", List.of(
                ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(GoogleGemini.builder()
                .type(GoogleGemini.class.getName())
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .build();

        ChatCompletion.Output output = task.run(runContext);

        assertThat(output.getAiResponse(), notNullValue());
        List<ChatCompletion.ChatMessage> updatedMessages = output.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatCompletion.ChatMessage.builder()
            .type(ChatCompletion.ChatMessageType.USER)
            .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "apiKey", GEMINI_APIKEY,
            "modelName", "gemini-1.5-flash",
            "messages", updatedMessages
        ));

        ChatCompletion secondTask = ChatCompletion.builder()
            .provider(GoogleGemini.builder()
                .type(GoogleGemini.class.getName())
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
        assertThat(secondOutput.getOutputMessages().size(), is(2));
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
                ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(Ollama.builder()
                .type(Ollama.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .endpoint(new Property<>("{{ ollamaEndpoint }}"))
                .build()
            )
            .build();

        ChatCompletion.Output output = task.run(runContext);

        assertThat(output.getAiResponse(), notNullValue());
        List<ChatCompletion.ChatMessage> updatedMessages = output.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatCompletion.ChatMessage.builder()
            .type(ChatCompletion.ChatMessageType.USER)
            .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "ollamaEndpoint", ollamaEndpoint,
            "messages", updatedMessages
            ));

        ChatCompletion secondTask = ChatCompletion.builder()
            .provider(Ollama.builder()
                .type(Ollama.class.getName())
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
        assertThat(secondOutput.getOutputMessages().size(), is(2));
    }


    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_APIKEY", matches = ".*")
    @Test
    void testChatCompletionAnthropicAI() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", AnthropicChatModelName.CLAUDE_3_HAIKU_20240307,
            "apiKey", ANTHROPIC_APIKEY,
            "messages", List.of(
                ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(AnthropicAI.builder()
                .type(AnthropicAI.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .build()
            )
            .build();

        ChatCompletion.Output output = task.run(runContext);

        assertThat(output.getAiResponse(), notNullValue());
        List<ChatCompletion.ChatMessage> updatedMessages = output.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatCompletion.ChatMessage.builder()
            .type(ChatCompletion.ChatMessageType.USER)
            .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "modelName", AnthropicChatModelName.CLAUDE_3_HAIKU_20240307,
            "apiKey", ANTHROPIC_APIKEY,
            "messages", updatedMessages
        ));

        ChatCompletion secondTask = ChatCompletion.builder()
            .provider(Ollama.builder()
                .type(AnthropicAI.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .messages(new Property<>("{{ messages }}"))
            .build();

        // WHEN: Run the second task
        ChatCompletion.Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), containsString("John"));
        assertThat(secondOutput.getOutputMessages().size(), is(2));
    }

    @Test
    void testChatCompletionAnthropicAI_givenInvalidApiKey_shouldThrow4xxUnAuthorizedException() {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", AnthropicChatModelName.CLAUDE_3_HAIKU_20240307,
            "apiKey", "ANTHROPIC_APIKEY",
            "messages", List.of(
                ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(AnthropicAI.builder()
                .type(AnthropicAI.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .build()
            )
            .build();

        // Assert RuntimeException and error message
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ChatCompletion.Output output = task.run(runContext);
        }, "status code: 401");

        // Verify error message contains 404 details
        assertThat(exception.getMessage(), containsString("authentication_error"));
    }


    @EnabledIfEnvironmentVariable(named = "MISTRAL_APIKEY", matches = ".*")
    @Test
    void testChatCompletionMistralAI() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "mistral:7b",
            "apiKey", "MISTRAL_APIKEY",
             "baseUrl", "https://api.mistral.ai/v1",
            "messages", List.of(
                ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(MistralAI.builder()
                .type(MistralAI.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .baseUrl(new Property<>("{{ baseUrl }}"))
                .build()
            )
            .build();

        ChatCompletion.Output output = task.run(runContext);

        assertThat(output.getAiResponse(), notNullValue());
        List<ChatCompletion.ChatMessage> updatedMessages = output.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatCompletion.ChatMessage.builder()
            .type(ChatCompletion.ChatMessageType.USER)
            .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "modelName", "mistral:7b",
            "apiKey", MISTRAL_APIKEY,
            "baseUrl", ollamaEndpoint,
            "messages", updatedMessages
        ));

        ChatCompletion secondTask = ChatCompletion.builder()
            .provider(MistralAI.builder()
                .type(MistralAI.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .baseUrl(new Property<>("{{ baseUrl }}"))
                .build()
            )
            .messages(new Property<>("{{ messages }}"))
            .build();

        // WHEN: Run the second task
        ChatCompletion.Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), containsString("John"));
        assertThat(secondOutput.getOutputMessages().size(), is(2));
    }

    @Test
    void testChatCompletionMistralAI_givenInvalidApiKey_shouldThrow4xxUnAuthorizedException() {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "mistral:7b",
            "apiKey", "MISTRAL_APIKEY",
            "baseUrl", "https://api.mistral.ai/v1",
            "messages", List.of(
                ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(MistralAI.builder()
                .type(MistralAI.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .baseUrl(new Property<>("{{ baseUrl }}"))
                .build()
            )
            .build();

        // Assert RuntimeException and error message
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ChatCompletion.Output output = task.run(runContext);
        }, "status code: 401");

        // Verify error message contains 404 details
        assertThat(exception.getMessage(), containsString("Unauthorized"));
    }

    @Test
    void testChatCompletionMistralAI_givenInvalidBaseUrlMistralAI_shouldThrow4xx() {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "mistral:7b",
            "apiKey", "MISTRAL_APIKEY",
            "baseUrl", ollamaEndpoint,
            "messages", List.of(
                ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(MistralAI.builder()
                .type(MistralAI.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .baseUrl(new Property<>("{{ baseUrl }}"))
                .build()
            )
            .build();

        // Assert RuntimeException and error message
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ChatCompletion.Output output = task.run(runContext);
        }, "status code: 404; body: 404 page not found");

        // Verify error message contains 404 details
        assertThat(exception.getMessage(), containsString("404"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_APIKEY", matches = ".*")
    void testChatCompletionDeepseek() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "apiKey", DEEPSEEK_APIKEY,
            "modelName", "deepseek-chat",
            "baseUrl", "https://api.deepseek.com/v1",
            "messages", List.of(
                ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(DeepseekAI.builder()
                .type(DeepseekAI.class.getName())
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .baseUrl(new Property<>("{{ baseUrl }}"))
                .build()
            )
            .build();

        ChatCompletion.Output output = task.run(runContext);

        assertThat(output.getAiResponse(), notNullValue());
        List<ChatCompletion.ChatMessage> updatedMessages = output.getOutputMessages();

        // GIVEN: Second prompt using the updated messages
        updatedMessages.add(ChatCompletion.ChatMessage.builder()
            .type(ChatCompletion.ChatMessageType.USER)
            .content("What's my name?")
            .build());

        runContext = runContextFactory.of(Map.of(
            "apiKey", DEEPSEEK_APIKEY,
            "modelName", "deepseek-chat",
            "baseUrl", "https://api.deepseek.com/v1",
            "messages", updatedMessages
        ));

        ChatCompletion secondTask = ChatCompletion.builder()
            .provider(DeepseekAI.builder()
                .type(DeepseekAI.class.getName())
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .baseUrl(new Property<>("{{ baseUrl }}"))
                .build()
            )
            .messages(new Property<>("{{ messages }}"))
            .build();

        // WHEN: Run the second task
        ChatCompletion.Output secondOutput = secondTask.run(runContext);

        // THEN: Validate the second response
        assertThat(secondOutput.getAiResponse(), containsString("John"));
        assertThat(secondOutput.getOutputMessages().size(), is(2));
    }

    @Test
    void testChatCompletionDeepseek_givenInvalidApiKey_shouldThrow4xxUnAuthorizedException() {
        RunContext runContext = runContextFactory.of(Map.of(
            "apiKey", "DEEPSEEK_APIKEY",
            "modelName", "deepseek-chat",
            "baseUrl", "https://api.deepseek.com/v1",
            "messages", List.of(
                ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("Hello, my name is John").build()
            )
        ));

        ChatCompletion task = ChatCompletion.builder()
            .messages(new Property<>("{{ messages }}"))
            .provider(DeepseekAI.builder()
                .type(DeepseekAI.class.getName())
                .modelName(new Property<>("{{ modelName }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .baseUrl(new Property<>("{{ baseUrl }}"))
                .build()
            )
            .build();

        // Assert RuntimeException and error message
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ChatCompletion.Output output = task.run(runContext);
        }, "status code: 401");

        // Verify error message contains 404 details
        assertThat(exception.getMessage(), containsString("Authentication Fails, Your api key: ****IKEY is invalid"));
    }
}
