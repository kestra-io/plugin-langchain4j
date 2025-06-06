package io.kestra.plugin.langchain4j.embeddings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.EmbeddingStoreProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Plugin(beta = true)
@JsonDeserialize
@Schema(
    title = "Chroma Embedding Store",
    description = "Always uses cosine distance as the distance metric"
)
public class Chroma extends EmbeddingStoreProvider {

    @NotNull
    @Schema(title = "The database base URL")
    private Property<String> baseUrl;

    @NotNull
    @Schema(title = "The collection name")
    private Property<String> collectionName;

    @Schema(title = "Whether the requests are logged or not.")
    private Property<Boolean> logRequests;

    @Schema(title = "Whether the responses are logged or not.")
    private Property<Boolean> logResponses;

    @Override
    public EmbeddingStore<TextSegment> embeddingStore(RunContext runContext, int dimension, boolean drop) throws IllegalVariableEvaluationException {

        // dimension is useless since the given embedding dimension will be used inside Chroma

        var renderedCollectionName = runContext.render(collectionName).as(String.class).orElseThrow();
        var store = ChromaEmbeddingStore.builder()
            .baseUrl(runContext.render(baseUrl).as(String.class).orElseThrow())
            .collectionName(renderedCollectionName)
            .logRequests(runContext.render(logRequests).as(Boolean.class).orElse(true))
            .logResponses(runContext.render(logResponses).as(Boolean.class).orElse(true))
            .build();

        if (drop) {
            store.removeAll();
        }

        return store;
    }
}
