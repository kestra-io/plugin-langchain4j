package io.kestra.plugin.langchain4j.embeddings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
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

import java.io.IOException;

@Getter
@SuperBuilder
@NoArgsConstructor
@Plugin(beta = true)
@JsonDeserialize
@Schema(
    title = "Pinecone Embedding Store"
)
public class Pinecone extends EmbeddingStoreProvider {

    @NotNull
    @Schema(title = "The API key")
    private Property<String> apiKey;

    @NotNull
    @Schema(title = "The cloud provider")
    private Property<String> cloud;

    @NotNull
    @Schema(title = "The cloud provider region")
    private Property<String> region;

    @NotNull
    @Schema(title = "The index")
    private Property<String> index;

    @Schema(title = "The namespace (default will be used if not provided)")
    private Property<String> namespace;

    @Override
    public EmbeddingStore<TextSegment> embeddingStore(RunContext runContext, int dimension, boolean drop) throws IOException, IllegalVariableEvaluationException {
        var store = PineconeEmbeddingStore.builder()
            .apiKey(runContext.render(apiKey).as(String.class).orElseThrow())
            .index(runContext.render(index).as(String.class).orElseThrow())
            .nameSpace(runContext.render(namespace).as(String.class).orElse(null)) // null will fallback to "default"
            .createIndex(PineconeServerlessIndexConfig.builder()
                .cloud(runContext.render(cloud).as(String.class).orElseThrow())
                .region(runContext.render(region).as(String.class).orElseThrow())
                .dimension(dimension)
                .build())
            .build();

        if (drop) {
            store.removeAll();
        }

        return store;
    }
}
