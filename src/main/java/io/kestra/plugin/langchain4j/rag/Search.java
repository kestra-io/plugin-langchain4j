package io.kestra.plugin.langchain4j.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.EmbeddingStoreProvider;
import io.kestra.plugin.langchain4j.domain.ModelProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Search using embeddings from a query against an embedding store.",
            code = """
                id: search_embeddings_flow
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

                  - id: search
                    type: io.kestra.plugin.langchain4j.rag.Search
                    provider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      modelName: gemini-embedding-exp-03-07
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddings:
                      type: io.kestra.plugin.langchain4j.embeddings.KestraKVStore
                    query: "Feature Highlights"
                """
        ),
    },
    beta = true
)
@Schema(
    title = "Search text into embeddings",
    description = "Performs a semantic search using a query string"
)
public class Search extends Task implements RunnableTask<Search.Output> {

    @Schema(title = "Query string to search for")
    @NotNull
    private Property<String> query;

    @Schema(title = "Maximum number of results to return")
    @Builder.Default
    private Property<Integer> maxResults = Property.ofValue(5);

    @Schema(title = "Minimum similarity score")
    @Builder.Default
    private Property<Double> minScore = Property.ofValue(0.8);

    @Schema(title = "The embedding model provider")
    @NotNull
    @PluginProperty
    private ModelProvider provider;

    @Schema(title = "The embedding store provider")
    @NotNull
    @PluginProperty
    private EmbeddingStoreProvider embeddings;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var embeddingModel = provider.embeddingModel(runContext);
        var store = embeddings.embeddingStore(runContext, embeddingModel.dimension(), false);

        var renderedQuery = runContext.render(query).as(String.class).orElseThrow();
        var embedding = embeddingModel.embed(renderedQuery).content();

        var request = EmbeddingSearchRequest.builder()
            .queryEmbedding(embedding)
            .maxResults(runContext.render(maxResults).as(Integer.class).orElse(0))
            .minScore(runContext.render(minScore).as(Double.class).orElse(0.0))
            .build();

        var results = store.search(request).matches().stream()
            .map(EmbeddingMatch::embedded)
            .map(TextSegment::text)
            .toList();

        return Output.builder()
            .results(results)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "List of matching text results")
        private final List<String> results;
    }
}
