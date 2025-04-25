package io.kestra.plugin.langchain4j.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.EmbeddingStoreProvider;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.IOException;

@Getter
@SuperBuilder
@NoArgsConstructor
@Plugin(beta = true)
@JsonDeserialize
public class PGVectorEmbeddingStore extends EmbeddingStoreProvider {
    @NotNull
    private Property<String> host;

    @NotNull
    private Property<Integer> port;

    @NotNull
    private Property<String> user;

    @NotNull
    private Property<String> password;

    @NotNull
    private Property<String> database;

    @NotNull
    private Property<String> table;

    @Override
    public EmbeddingStore<TextSegment> embeddingStore(RunContext runContext, int dimension) throws IOException, IllegalVariableEvaluationException {
        return PgVectorEmbeddingStore.builder()
            .host(runContext.render(host).as(String.class).orElseThrow())
            .port(runContext.render(port).as(Integer.class).orElseThrow())
            .database(runContext.render(database).as(String.class).orElseThrow())
            .user(runContext.render(user).as(String.class).orElseThrow())
            .password(runContext.render(password).as(String.class).orElseThrow())
            .table(runContext.render(table).as(String.class).orElseThrow())
            .dimension(dimension)
            .build();
    }
}
