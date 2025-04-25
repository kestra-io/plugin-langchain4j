package io.kestra.plugin.langchain4j;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Retrieval Augmented Generation")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = """
                A Retrieval Augmented Generation (RAG) pipeline that first index documents, then use the RAG task to call an LLM with embedding retrieval.
                WARNING: the KV embedding store is for quick prototyping only as it store the embedding vectors in a K/V store an load them all in memory.
                """,
            code = """
                id: rag
                namespace: company.team

                tasks:
                  - id: ingest
                    type: io.kestra.plugin.langchain4j.IndexDocument
                    provider:
                      type: io.kestra.plugin.langchain4j.model.GeminiModelProvider
                      modelName: gemini-embedding-exp-03-07
                      apiKey: your_api_key
                    embeddingStore:
                      type: io.kestra.plugin.langchain4j.store.KvEmbeddingStore
                    fromDocuments:
                      - content: My name is Loïc
                      - content: I live in Lille
                      - content: My tailor is rich
                  - id: rag
                    type: io.kestra.plugin.langchain4j.RAG
                    chatModelProvider:
                      type: io.kestra.plugin.langchain4j.model.GeminiModelProvider
                      modelName: gemini-1.5-flash
                      apiKey: your_api_key
                    embeddingModelProvider:
                      type: io.kestra.plugin.langchain4j.model.GeminiModelProvider
                      modelName: gemini-embedding-exp-03-07
                      apiKey: your_api_key
                    embeddingStore:
                      type: io.kestra.plugin.langchain4j.store.KvEmbeddingStore
                    prompt: Hello AI!
                """
        )
    },
    beta = true
)
public class RAG  extends Task implements RunnableTask<RAG.Output> {
    @Schema(title = "Text prompt", description = "The input prompt for the language model")
    @NotNull
    protected Property<String> prompt;

    @Schema(title = "Embedding Store")
    @NotNull
    @PluginProperty
    private EmbeddingStoreProvider embeddingStore;

    @Schema(
        title = "Embedding Store Model Provider",
        description = "Optional, if not set the embedding model will be created by the `chatModelProvider`. In this case, be sure that the `chatModelProvider` supports embeddings."
    )
    @PluginProperty
    private ModelProvider embeddingModelProvider;

    @Schema(title = "Chat Model Provider")
    @NotNull
    @PluginProperty
    private ModelProvider chatModelProvider;

    @Schema(title = "Chat configuration")
    @NotNull
    @PluginProperty
    @Builder.Default
    private ChatConfiguration chatConfiguration = ChatConfiguration.empty();

    @NotNull
    @PluginProperty
    @Builder.Default
    private ContentRetrieverConfiguration contentRetrieverConfiguration = ContentRetrieverConfiguration.builder().build();

    @Override
    public Output run(RunContext runContext) throws Exception {
        var embeddingModel = Optional.ofNullable(embeddingModelProvider).orElse(chatModelProvider).embeddingModel(runContext);
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore.embeddingStore(runContext, embeddingModel.dimension()))
            .maxResults(contentRetrieverConfiguration.getMaxResults())
            .minScore(contentRetrieverConfiguration.getMinScore())
            .build();

        Assistant assistant = AiServices.builder(Assistant.class)
            .chatLanguageModel(chatModelProvider.chatLanguageModel(runContext, chatConfiguration))
            .contentRetriever(contentRetriever)
            .build();

        String renderedPrompt = runContext.render(prompt).as(String.class).orElseThrow();
        String completion = assistant.chat(renderedPrompt);
        runContext.logger().debug("Generated Completion: {}", completion);

        return Output.builder()
            .completion(completion)
            .build();
    }

    interface Assistant {
        String chat(String userMessage);
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
