package io.kestra.plugin.ai.embeddings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.ai.domain.EmbeddingStoreProvider;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.IOException;

@Getter
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize
@Schema(
    title = "Milvus Embedding Store"
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Ingest documents into a Milvus embedding store.",
            code = """
                id: document-ingestion
                namespace: company.team

                tasks:
                  - id: ingest
                    type: io.kestra.plugin.ai.rag.IngestDocument
                    provider:
                      type: io.kestra.plugin.ai.provider.GoogleGemini
                      modelName: gemini-embedding-exp-03-07
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddings:
                      type: io.kestra.plugin.ai.embeddings.Milvus
                      token: "{{ secret('MILVUS_TOKEN') }}"
                      uri: "http://localhost:19200"
                      collectionName: embeddings
                    fromExternalURLs:
                      - https://raw.githubusercontent.com/kestra-io/docs/refs/heads/main/content/blogs/release-0-22.md
                """
        )
    },
    aliases = "io.kestra.plugin.langchain4j.embeddings.Milvus"
)
public class Milvus extends EmbeddingStoreProvider {

    @Schema(title = "The token")
    @NotNull
    private Property<String> token;

    @Schema(title = "The uri")
    private Property<String> uri;

    @Schema(title = "The host", description = "Default value: \"localhost\"")
    private Property<String> host;

    @Schema(title = "The port", description = "Default value: \"19530\"")
    private Property<Integer> port;

    @Schema(
        title = "The username",
        description = "If user authentication and TLS is enabled, this parameter is required. See: https://milvus.io/docs/authenticate.md"
    )
    private Property<String> username;

    @Schema(
        title = "The password",
        description = "If user authentication and TLS is enabled, this parameter is required. See: https://milvus.io/docs/authenticate.md"
    )
    private Property<String> password;

    @Schema(
        title = "The collection name",
        description = "If there is no such collection yet, it will be created automatically. Default value: \"default\"."
    )
    private Property<String> collectionName;

    @Schema(title = "The consistency level")
    private Property<String> consistencyLevel;

    @Schema(title = "The index type")
    private Property<String> indexType;

    @Schema(title = "The metric type")
    private Property<String> metricType;

    @Schema(title = "Whether to retrieve embeddings on search")
    private Property<Boolean> retrieveEmbeddingsOnSearch;

    @Schema(title = "The database name", description = "If not provided, the default database will be used.")
    private Property<String> databaseName;

    @Schema(title = "Whether to auto flush on insert")
    private Property<Boolean> autoFlushOnInsert;

    @Schema(title = "Whether to auto flush on delete")
    private Property<Boolean> autoFlushOnDelete;

    @Schema(title = "The id field name")
    private Property<String> idFieldName;

    @Schema(title = "The text field name")
    private Property<String> textFieldName;

    @Schema(title = "The metadata field name")
    private Property<String> metadataFieldName;

    @Schema(title = "The vector field name")
    private Property<String> vectorFieldName;

    @Override
    public EmbeddingStore<TextSegment> embeddingStore(RunContext runContext, int dimension, boolean drop) throws IOException, IllegalVariableEvaluationException {
        var store = MilvusEmbeddingStore.builder()
            .token(runContext.render(token).as(String.class).orElseThrow())
            .uri(runContext.render(uri).as(String.class).orElse(null))
            .host(runContext.render(host).as(String.class).orElse(null))
            .port(runContext.render(port).as(Integer.class).orElse(null))
            .username(runContext.render(username).as(String.class).orElse(null))
            .password(runContext.render(password).as(String.class).orElse(null))
            .collectionName(runContext.render(collectionName).as(String.class).orElse(null))
            .consistencyLevel(ConsistencyLevelEnum.valueOf(runContext.render(consistencyLevel).as(String.class).orElse("EVENTUALLY")))
            .indexType(IndexType.valueOf(runContext.render(indexType).as(String.class).orElse("FLAT")))
            .metricType(MetricType.valueOf(runContext.render(metricType).as(String.class).orElse("COSINE")))
            .retrieveEmbeddingsOnSearch(runContext.render(retrieveEmbeddingsOnSearch).as(Boolean.class).orElse(false))
            .databaseName(runContext.render(databaseName).as(String.class).orElse(null))
            .autoFlushOnInsert(runContext.render(autoFlushOnInsert).as(Boolean.class).orElse(false))
            .idFieldName(runContext.render(idFieldName).as(String.class).orElse(null))
            .textFieldName(runContext.render(textFieldName).as(String.class).orElse(null))
            .metadataFieldName(runContext.render(metadataFieldName).as(String.class).orElse(null))
            .vectorFieldName(runContext.render(vectorFieldName).as(String.class).orElse(null))
            .dimension(dimension)
            .build();


        if (drop) {
            store.removeAll();
        }

        return store;
    }
}
