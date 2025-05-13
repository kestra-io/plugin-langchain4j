package io.kestra.plugin.langchain4j.embeddings;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.Output;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.models.ModelProvider;
import io.kestra.plugin.langchain4j.stores.EmbeddingStoreProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

@SuperBuilder
@Getter
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Plugin(
    id = "search",
    description = "Search using embeddings from a query against an embedding store"
)
@Schema(
    title = "Embedding Search",
    description = "Performs a semantic search using a query string and embedding store"
)
public class Search extends Task implements RunnableTask<Search.Output> {

    @Schema(title = "Query string to search for")
    private String query;

    @Schema(title = "Maximum number of results to return")
    @Builder.Default
    private Integer maxResults = 5;

    @Schema(title = "Minimum similarity score")
    @Builder.Default
    private Double minScore = 0.8;

    @Schema(title = "The embedding model provider")
    private ModelProvider embeddingProvider;

    @Schema(title = "The embedding store provider")
    private EmbeddingStoreProvider embeddings;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        EmbeddingModel model = embeddingProvider.getEmbeddingModel();
        EmbeddingStore<String> store = embeddings.get();

        Embedding embedding = model.embed(query).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
            .embedding(embedding)
            .maxResults(maxResults)
            .minScore(minScore)
            .build();

        List<EmbeddingMatch<String>> results = store.search(request);
        List<String> texts = results.stream()
            .map(EmbeddingMatch::embedded)
            .collect(Collectors.toList());

        return Output.builder()
            .results(texts)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements Output {
        @Schema(title = "List of matching text results")
        private final List<String> results;
    }
}
