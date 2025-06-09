package io.kestra.plugin.langchain4j.rag;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                      modelName: gemini-1.5-flash
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    messages:
                    - type: user
                      content: Which features were released in Kestra 0.22?

                  - id: chat_with_rag
                    type: io.kestra.plugin.langchain4j.rag.ChatCompletion
                    chatProvider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-1.5-flash
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
                      modelName: gemini-1.5-flash
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    contentRetrievers:
                    - type: io.kestra.plugin.langchain4j.retriever.GoogleCustomWebSearch
                      apiKey: "{{ secret('GOOGLE_API_KEY') }}"
                      csi: "{{ secret('GOOGLE_CSI_KEY') }}"
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
                      modelName: gemini-1.5-flash
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddingProvider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-embedding-exp-03-07
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddings:
                      type: io.kestra.plugin.langchain4j.embeddings.KestraKVStore
                    tools:
                    - type: io.kestra.plugin.langchain4j.tool.GoogleCustomWebSearch
                      apiKey: "{{ secret('GOOGLE_API_KEY') }}"
                      csi: "{{ secret('GOOGLE_CSI_KEY') }}"
                    prompt: What is the latest release of Kestra?
                """
        )
    },
    beta = true
)
public class ChatCompletion extends Task implements RunnableTask<ChatCompletion.Output> {
    @Schema(title = "Text prompt", description = "The input prompt for the language model")
    @NotNull
    protected Property<String> prompt;

    @Schema(
        title = "Embedding Store Provider",
        description = "Optional if at least one `contentRetrievers` is provided"
    )
    @PluginProperty
    private EmbeddingStoreProvider embeddings;

    @Schema(
        title = "Embedding Store Model Provider",
        description = "Optional, if not set, the embedding model will be created by the `chatModelProvider`. In this case, be sure that the `chatModelProvider` supports embeddings."
        )
    @PluginProperty
    private ModelProvider embeddingProvider;

    @Schema(title = "Chat Model Provider")
    @NotNull
    @PluginProperty
    private ModelProvider chatProvider;

    @Schema(title = "Chat configuration")
    @NotNull
    @PluginProperty
    @Builder.Default
    private ChatConfiguration chatConfiguration = ChatConfiguration.empty();

    @Schema(title = "Content Retriever Configuration")
    @NotNull
    @PluginProperty
    @Builder.Default
    private ContentRetrieverConfiguration contentRetrieverConfiguration = ContentRetrieverConfiguration.builder().build();

    @Schema(
        title = "Additional content retrievers",
        description = "Some content retrievers like WebSearch can be used also as tools, but using them as content retrievers will make them always used whereas tools are only used when the LLM decided to."
    )
    private Property<List<ContentRetrieverProvider>> contentRetrievers;

    @Schema(title = "Tools that the LLM may use to augment its response")
    private Property<List<ToolProvider>> tools;

    @Override
    public Output run(RunContext runContext) throws Exception {
        List<ToolProvider> toolProviders = runContext.render(tools).asList(ToolProvider.class);

        try {
            Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatProvider.chatModel(runContext, chatConfiguration))
                .retrievalAugmentor(buildRetrievalAugmentor(runContext))
                .tools(buildTools(runContext, toolProviders))
                .build();

            String renderedPrompt = runContext.render(prompt).as(String.class).orElseThrow();
            Response<AiMessage> completion = assistant.chat(renderedPrompt);
            runContext.logger().debug("Generated Completion: {}", completion.content());

            return Output.builder()
                .completion(completion.content().text())
                .tokenUsage(completion.tokenUsage())
                .finishReason(completion.finishReason())
                .build();
        } finally {
            toolProviders.forEach(tool -> tool.close(runContext));
        }
    }

    private List<ToolSpecification> buildTools(RunContext runContext, List<ToolProvider> toolProviders) throws IllegalVariableEvaluationException {
        return toolProviders.stream()
            .flatMap(throwFunction(provider -> provider.tool(runContext).stream()))
            .toList();
    }

    private RetrievalAugmentor buildRetrievalAugmentor(final RunContext runContext) throws Exception {
        List<ContentRetriever> toolContentRetrievers = runContext.render(contentRetrievers).asList(ContentRetrieverProvider.class).stream()
            .map(throwFunction(provider -> provider.contentRetriever(runContext)))
            .collect(Collectors.toList());

        Optional<ContentRetriever> contentRetriever = Optional.ofNullable(embeddings).map(throwFunction(
            embeddings -> {
                var embeddingModel = Optional.ofNullable(embeddingProvider).orElse(chatProvider).embeddingModel(runContext);
                return EmbeddingStoreContentRetriever.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddings.embeddingStore(runContext, embeddingModel.dimension(), false))
                    .maxResults(contentRetrieverConfiguration.getMaxResults())
                    .minScore(contentRetrieverConfiguration.getMinScore())
                    .build();
            }));

        if (toolContentRetrievers.isEmpty() && contentRetriever.isEmpty()) {
            throw new IllegalArgumentException("Either `embeddings` or `contentRetrievers` must be provided.");
        }

        if (toolContentRetrievers.isEmpty()) {
            return DefaultRetrievalAugmentor.builder().contentRetriever(contentRetriever.get()).build();
        } else {
            // always add it first so it has precedence over the additional content retrievers
            contentRetriever.ifPresent(ct -> toolContentRetrievers.addFirst(ct));
            QueryRouter queryRouter = new DefaultQueryRouter(toolContentRetrievers.toArray(new ContentRetriever[0]));

            // Create a query router that will route each query to the embedding store content retriever and the tools content retrievers
            return DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();
        }
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

    @Builder
    @Getter
    public static class ContentRetrieverConfiguration {
        @Schema(title = "The maximum number of results from the embedding store.")
        @Builder.Default
        private Integer maxResults = 3;

        @Schema(title = "The minimum score, ranging from 0 to 1 (inclusive). Only embeddings with a score >= minScore will be returned.")
        @Builder.Default
        private Double minScore = 0.0D;
    }
}
