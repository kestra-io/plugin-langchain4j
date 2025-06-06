package io.kestra.plugin.langchain4j.embeddings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
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
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@Plugin(beta = true)
@JsonDeserialize
@Schema(
    title = "Weaviate Embedding Store"
)
public class Weaviate extends EmbeddingStoreProvider {

    @Schema(
        title = "Weaviate API key",
        description = "Your Weaviate API key. Not required for local deployment."
    )
    @NotNull
    private Property<String> apiKey;

    @Schema(
        title = "Weaviate host",
        description = "The host, e.g. \"langchain4j-4jw7ufd9.weaviate.network\" of cluster URL. Find in under Details of your Weaviate cluster."
    )
    @NotNull
    private Property<String> host;

    @Schema(
        title = "Weaviate port",
        description = "The port, e.g. 8080. This parameter is optional."
    )
    private Property<Integer> port;

    @Schema(
        title = "Weaviate object class",
        description = "The object class you want to store, e.g. \"MyGreatClass\". Must start from an uppercase letter. If not provided, will default to \"Default\"."
    )
    private Property<String> objectClass;

    @Schema(
        title = "Weaviate consistency level",
        description = "Consistency level: ONE, QUORUM (default) or ALL."
    )
    private Property<ConsistencyLevel> consistencyLevel;

    @Schema(
        title = "Weaviate avoid dups",
        description = "If true (default), then WeaviateEmbeddingStore will generate a hashed ID based on provided text segment, " +
            "which avoids duplicated entries in DB. If false, then random ID will be generated."
    )
    private Property<Boolean> avoidDups;

    @Schema(
        title = "Weaviate metadata field name",
        description = "The name of the metadata field to store. If not provided, will default to \"_metadata\"."
    )
    private Property<String> metadataFieldName;

    @Schema(
        title = "Weaviate metadata keys",
        description = "The list of metadata keys to store. If not provided, will default to an empty list."
    )
    private Property<List<String>> metadataKeys;

    @Schema(
        title = "Use gRPC for inserts",
        description = "Use GRPC instead of HTTP for batch inserts only. You still need HTTP configured for search."
    )
    private Property<Boolean> useGrpcForInserts;

    @Schema(title = "The gRPC connection is secured")
    private Property<Boolean> securedGrpc;

    @Schema(title = "gRPC port if used")
    private Property<Integer> grpcPort;

    @Override
    public EmbeddingStore<TextSegment> embeddingStore(RunContext runContext, int dimension, boolean drop) throws IOException, IllegalVariableEvaluationException {

        // dimension is useless since the given embedding dimension will be used inside Qdrant

        var store = WeaviateEmbeddingStore.builder()
            .apiKey(runContext.render(apiKey).as(String.class).orElseThrow())
            .scheme("FIXME")
            .host(runContext.render(host).as(String.class).orElseThrow())
            .port(runContext.render(port).as(Integer.class).orElseThrow())
            .objectClass(runContext.render(objectClass).as(String.class).orElseThrow())
            .avoidDups(runContext.render(avoidDups).as(Boolean.class).orElse(true))
            .consistencyLevel(runContext.render(consistencyLevel).as(ConsistencyLevel.class).orElse(ConsistencyLevel.QUORUM).name())
            .metadataFieldName(runContext.render(metadataFieldName).as(String.class).orElse(null))
            .metadataKeys(runContext.render(metadataKeys).asList(String.class))
            .useGrpcForInserts(runContext.render(useGrpcForInserts).as(Boolean.class).orElse(false))
            .securedGrpc(runContext.render(securedGrpc).as(Boolean.class).orElse(true))
            .grpcPort(runContext.render(grpcPort).as(Integer.class).orElse(null))
            .build();

        if (drop) {
            store.removeAll();
        }

        return store;
    }

    enum ConsistencyLevel {
        ONE,
        QUORUM,
        ALL,
    }
}
