package io.kestra.plugin.langchain4j.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.storages.kv.KVStore;
import io.kestra.core.storages.kv.KVValue;
import io.kestra.core.storages.kv.KVValueAndMetadata;
import io.kestra.plugin.langchain4j.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.util.List;

import static io.kestra.core.utils.Rethrow.throwFunction;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Create a Retrieval Augmented Generation (RAG) pipeline.")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = """
                Chat with your data using Retrieval Augmented Generation (RAG). This flow will index documents and use the RAG Chat task to interact with your data using natural language prompts. The flow contrasts prompts to LLM with and without RAG. The Chat with RAG retrieves embeddings stored in the KV Store and provides a response grounded in data rather than hallucinating.
                WARNING: the KV embedding store is for quick prototyping only, as it stores the embedding vectors in Kestra's KV store an loads them all into memory.
                """,
            code = """
                id: rag
                namespace: company.team

                tasks:
                  - id: ingest
                    type: io.kestra.plugin.langchain4j.rag.IngestDocument
                    provider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-embedding-exp-03-07
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddings:
                      type: io.kestra.plugin.langchain4j.embeddings.KestraKVStore
                    drop: true
                    fromExternalURLs:
                      - https://raw.githubusercontent.com/kestra-io/docs/refs/heads/main/content/blogs/release-0-22.md

                  - id: chat_without_rag
                    type: io.kestra.plugin.langchain4j.ChatCompletion
                    provider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-2.0-flash
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    messages:
                    - type: user
                      content: Which features were released in Kestra 0.22?

                  - id: chat_with_rag
                    type: io.kestra.plugin.langchain4j.rag.ChatCompletion
                    chatProvider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-2.0-flash
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddingProvider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-embedding-exp-03-07
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddings:
                      type: io.kestra.plugin.langchain4j.embeddings.KestraKVStore
                    prompt: Which features were released in Kestra 0.22?
                """
        ),
        @Example(
            full = true,
            title = "Chat with your data using Retrieval Augmented Generation (RAG) and a WebSearch content retriever. The Chat with RAG retrieves contents from a WebSearch client and provides a response grounded in data rather than hallucinating.",
            code = """
                id: rag
                namespace: company.team

                tasks:
                  - id: chat_with_rag_and_websearch_content_retriever
                    type: io.kestra.plugin.langchain4j.rag.ChatCompletion
                    chatProvider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-2.0-flash
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    contentRetrievers:
                    - type: io.kestra.plugin.langchain4j.retriever.GoogleCustomWebSearch
                      apiKey: "{{ secret('GOOGLE_SEARCH_API_KEY') }}"
                      csi: "{{ secret('GOOGLE_SEARCH_CSI') }}"
                    prompt: What is the latest release of Kestra?
                """
        ),
        @Example(
            full = true,
            title = """
                Chat with your data using Retrieval Augmented Generation (RAG) and an additional WebSearch tool. This flow will index documents and use the RAG Chat task to interact with your data using natural language prompts. The flow contrasts prompts to LLM with and without RAG. The Chat with RAG retrieves embeddings stored in the KV Store and provides a response grounded in data rather than hallucinating. It may also include results from a web search engine if using the provided tool.
                WARNING: the KV embedding store is for quick prototyping only, as it stores the embedding vectors in Kestra's KV store an loads them all into memory.
                """,
            code = """
                id: rag
                namespace: company.team

                tasks:
                  - id: ingest
                    type: io.kestra.plugin.langchain4j.rag.IngestDocument
                    provider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-embedding-exp-03-07
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddings:
                      type: io.kestra.plugin.langchain4j.embeddings.KestraKVStore
                    drop: true
                    fromExternalURLs:
                      - https://raw.githubusercontent.com/kestra-io/docs/refs/heads/main/content/blogs/release-0-22.md

                  - id: chat_with_rag_and_tool
                    type: io.kestra.plugin.langchain4j.rag.ChatCompletion
                    chatProvider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-2.0-flash
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddingProvider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-embedding-exp-03-07
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddings:
                      type: io.kestra.plugin.langchain4j.embeddings.KestraKVStore
                    tools:
                    - type: io.kestra.plugin.langchain4j.tool.GoogleCustomWebSearch
                      apiKey: "{{ secret('GOOGLE_SEARCH_API_KEY') }}"
                      csi: "{{ secret('GOOGLE_SEARCH_CSI') }}"
                    prompt: What is the latest release of Kestra?
                """
        )
    },
    beta = true
)
public class AiAgent extends Task implements RunnableTask<AiAgent.Output> {
    @Schema(title = "System message", description = "The system message for the language model")
    @NotNull
    protected Property<String> systemMessage;

    @Schema(title = "Text prompt", description = "The input prompt for the language model")
    @NotNull
    protected Property<String> prompt;

    @Schema(title = "Chat Model Provider")
    @NotNull
    @PluginProperty
    private ModelProvider chatProvider;

    @Schema(title = "Chat configuration")
    @NotNull
    @PluginProperty
    @Builder.Default
    private ChatConfiguration chatConfiguration = ChatConfiguration.empty();

    @Schema(title = "Tools that the LLM may use to augment its response")
    private Property<List<ToolProvider>> tools;

    @Schema(
        title = "Agent Memory",
        description = "Agent memory will store messages and add them as history inside the LLM context."
    )
    @PluginProperty
    private MemoryProvider memory;

    @Override
    public Output run(RunContext runContext) throws Exception {
        List<ToolProvider> toolProviders = runContext.render(tools).asList(ToolProvider.class);

        ChatMemory chatMemory;
        if (memory != null) {
            chatMemory = memory.chatMemory(runContext);
        } else {
            // null memory is not allowed, so we use an in-memory memory with a capacity of two to support both system and user message
            chatMemory = MessageWindowChatMemory.withMaxMessages(2);
        }

        try {
            Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatProvider.chatModel(runContext, chatConfiguration))
                .tools(buildTools(runContext, toolProviders))
                .systemMessageProvider(throwFunction(memoryId -> runContext.render(systemMessage).as(String.class).orElse(null)))
                .chatMemory(chatMemory)
                .build();

            String renderedPrompt = runContext.render(prompt).as(String.class).orElseThrow();
            Response<AiMessage> completion = assistant.chat(renderedPrompt);
            runContext.logger().debug("Generated Completion: {}", completion.content());

            return Output.builder()
                .completion(completion.content().text())
                .tokenUsage(TokenUsage.from(completion.tokenUsage()))
                .finishReason(completion.finishReason())
                .build();
        } finally {
            toolProviders.forEach(tool -> tool.close(runContext));

            if (memory != null) {
                memory.close(runContext);
            }
        }
    }

    private List<ToolSpecification> buildTools(RunContext runContext, List<ToolProvider> toolProviders) throws IllegalVariableEvaluationException {
        return toolProviders.stream()
            .flatMap(throwFunction(provider -> provider.tool(runContext).stream()))
            .toList();
    }

    interface Assistant {
        Response<AiMessage> chat(String userMessage);
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Generated text completion", description = "The result of the text completion")
        private String completion;

        @Schema(title = "Token usage")
        private TokenUsage tokenUsage;

        @Schema(title = "Finish reason")
        private FinishReason finishReason;
    }

    // FIXME remove this note
    // n8n support as memory: in-memory which didn't work if you run multiple instances!, MongoDB, Redis and Postgres
    // as we already have a bunch of vector stores available, we could use their client to store memory in all supported vector databases...
    // for that, we would need the chat memory to be a plugin
    @SuperBuilder
    @Getter
    @Jacksonized
    public static class Memory {
        @Schema(title = "Whether memory is enabled")
        @Builder.Default
        private Property<Boolean> enabled = Property.ofValue(false);

        @Schema(title = "The maximum number of messages to keep inside the memory.")
        @Builder.Default
        private Property<Integer> messages = Property.ofValue(10);

        @Schema(title = "The memory id. Defaults to the value of the 'system.correlationId' label. This means that a memory is valid for the whole flow execution including its subflows.")
        @Builder.Default
        private Property<String> memoryId = Property.ofExpression("{{ flow.id }}-{{ labels.system.correlationId }}");

        @Schema(title = "The memory duration. Defaults to 60mn.")
        @Builder.Default
        private Property<Duration> ttl = Property.ofValue(Duration.ofMinutes(60));
    }
}
