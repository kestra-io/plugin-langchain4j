package io.kestra.plugin.langchain4j.rag;

import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
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
import io.kestra.plugin.langchain4j.domain.ChatConfiguration;
import io.kestra.plugin.langchain4j.domain.EmbeddingStoreProvider;
import io.kestra.plugin.langchain4j.domain.ModelProvider;
import io.kestra.plugin.langchain4j.tool.WebSearchTool;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Objects;
import java.util.Optional;

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
                    webSearchTool:
                      type: io.kestra.plugin.langchain4j.tool.GoogleCustomWebSearchTool
                      apiKey: "{{ secret('GOOGLE_API_KEY') }}"
                      csi: "{{ secret('GOOGLE_CSI_KEY') }}"

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
                    webSearchTool:
                      type: io.kestra.plugin.langchain4j.tool.TavilyWebSearchTool
                      apiKey: "{{ secret('TAVILY_API_KEY') }}"
                """
        )
    },
    beta = true
)
public class ChatCompletion extends Task implements RunnableTask<ChatCompletion.Output> {
    @Schema(title = "Text prompt", description = "The input prompt for the language model")
    @NotNull
    protected Property<String> prompt;

    @Schema(title = "Embedding Store Provider")
    @NotNull
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

    @NotNull
    @PluginProperty
    @Builder.Default
    private ContentRetrieverConfiguration contentRetrieverConfiguration = ContentRetrieverConfiguration.builder().build();

    @Schema(title = "Web Search Tool (used for retrieval augmentation)",
        description = "Optional, if set, the search engine will be used to retrieve additional information from the web. If not set, only the embedding store will be used.")
    @Nullable
    private WebSearchTool webSearchTool;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var embeddingModel = Optional.ofNullable(embeddingProvider).orElse(chatProvider).embeddingModel(runContext);
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddings.embeddingStore(runContext, embeddingModel.dimension(), false))
            .maxResults(contentRetrieverConfiguration.getMaxResults())
            .minScore(contentRetrieverConfiguration.getMinScore())
            .build();

        Assistant assistant = AiServices.builder(Assistant.class)
            .chatModel(chatProvider.chatModel(runContext, chatConfiguration))
            .retrievalAugmentor(buildRetrievalAugmentor(runContext, contentRetriever))
            .build();

        String renderedPrompt = runContext.render(prompt).as(String.class).orElseThrow();
        String completion = assistant.chat(renderedPrompt);
        runContext.logger().debug("Generated Completion: {}", completion);

        return Output.builder()
            .completion(completion)
            .build();
    }

    private RetrievalAugmentor buildRetrievalAugmentor(final RunContext runContext,  ContentRetriever contentRetriever) throws IllegalVariableEvaluationException {

        // If no search engine is provided, we only use the content retriever
        WebSearchContentRetriever webSearchContentRetriever = null;
        if (Objects.nonNull(webSearchTool)) {
            webSearchContentRetriever = webSearchTool.from(runContext);
            if (webSearchContentRetriever == null) {
                runContext.logger().warn("Web search content retriever is null, it will not be used in the query router.");
            }
        }
        // Create a query router that will route each query to the content retriever and the web search content retriever
        return DefaultRetrievalAugmentor.builder()
            .queryRouter(new KestraQueryRouter(contentRetriever, webSearchContentRetriever).getQueryRouter())
            .build();
    }

    interface Assistant {
        String chat(String userMessage);
    }

    private static class KestraQueryRouter {
        private QueryRouter queryRouter;

        public KestraQueryRouter(ContentRetriever... initialRetrievers) {
            // Initialize the query router with the provided content retrievers
            //remove null retrievers
            initialRetrievers = java.util.Arrays.stream(initialRetrievers)
                .filter(java.util.Objects::nonNull)
                .toArray(ContentRetriever[]::new);
            this.queryRouter = new DefaultQueryRouter(initialRetrievers);
        }

        public QueryRouter getQueryRouter() {
            return this.queryRouter;
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Generated text completion", description = "The result of the text completion")
        private final String completion;
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
