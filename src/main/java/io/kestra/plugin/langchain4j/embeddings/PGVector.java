package io.kestra.plugin.langchain4j.embeddings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.EmbeddingStoreProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.IOException;

@Getter
@SuperBuilder
@NoArgsConstructor
@Plugin(beta = true)
@JsonDeserialize
@Schema(
    title = "PGVector Embedding Store"
)
public class PGVector extends EmbeddingStoreProvider {
    @NotNull
    @Schema(title = "The database server host")
    private Property<String> host;

    @NotNull
    @Schema(title = "The database server port")
    private Property<Integer> port;

    @NotNull
    @Schema(title = "The database user")
    private Property<String> user;

    @NotNull
    @Schema(title = "The database password")
    private Property<String> password;

    @NotNull
    @Schema(title = "The database name")
    private Property<String> database;

    @NotNull
    @Schema(title = "The table to store embeddings in")
    private Property<String> table;

    @Schema(
        title = "Whether to use use an IVFFlat index",
        description = "An IVFFlat index divides vectors into lists, and then searches a subset of those lists closest to the query vector. It has faster build times and uses less memory than HNSW but has lower query performance (in terms of speed-recall tradeoff)."
    )
    @Builder.Default
    private Property<Boolean> useIndex = Property.ofValue(false);

    @Override
    public EmbeddingStore<TextSegment> embeddingStore(RunContext runContext, int dimension, boolean drop) throws IllegalVariableEvaluationException {
        return PgVectorEmbeddingStore.builder()
            .host(runContext.render(host).as(String.class).orElseThrow())
            .port(runContext.render(port).as(Integer.class).orElseThrow())
            .database(runContext.render(database).as(String.class).orElseThrow())
            .user(runContext.render(user).as(String.class).orElseThrow())
            .password(runContext.render(password).as(String.class).orElseThrow())
            .table(runContext.render(table).as(String.class).orElseThrow())
            .dropTableFirst(drop)
            .dimension(dimension)
            .useIndex(runContext.render(useIndex).as(Boolean.class).orElseThrow())
            .build();
    }
}
