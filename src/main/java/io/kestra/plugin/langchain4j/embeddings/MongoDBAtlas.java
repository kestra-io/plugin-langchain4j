package io.kestra.plugin.langchain4j.embeddings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mongodb.client.MongoClients;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.mongodb.IndexMapping;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@SuperBuilder
@NoArgsConstructor
@Plugin(beta = true)
@JsonDeserialize
@Schema(
    title = "MongoDB Atlas Embedding Store"
)
public class MongoDBAtlas extends EmbeddingStoreProvider {

    @Schema(title = "The username")
    private Property<String> username;

    @Schema(title = "The password")
    private Property<String> password;

    @NotNull
    @Schema(title = "The host")
    private Property<String> host;

    @Schema(title = "The database")
    private Property<String> database;

    @Schema(title = "The connection string options")
    private Property<Map<String, Object>> options;

    @NotNull
    @Schema(title = "The collection name")
    private Property<String> collectionName;

    @NotNull
    @Schema(title = "The index name")
    private Property<String> indexName;

    @Schema(title = "The metadata field names")
    private Property<List<String>> metadataFieldNames;

    @Schema(title = "Create the index")
    private Property<Boolean> createIndex;

    @Override
    public EmbeddingStore<TextSegment> embeddingStore(RunContext runContext, int dimension, boolean drop) throws IOException, IllegalVariableEvaluationException {

        var mongoClient = MongoClients.create(buildUri(runContext));

        var renderedCreateIndex = runContext.render(createIndex).as(Boolean.class).orElse(false);
        var store = MongoDbEmbeddingStore.builder()
            .fromClient(mongoClient)
            .databaseName(runContext.render(database).as(String.class).orElseThrow())
            .collectionName(runContext.render(collectionName).as(String.class).orElseThrow())
            .indexName(runContext.render(indexName).as(String.class).orElseThrow())
            .createIndex(renderedCreateIndex)
            .indexMapping(
                metadataFieldNames != null ?
                    IndexMapping.builder()
                        .dimension(dimension)
                        .metadataFieldNames(new HashSet<>(runContext.render(metadataFieldNames).asList(String.class)))
                        .build() : null
            )
            .build();

        if (renderedCreateIndex) {
            // Creating a vector search index can take up to a minute, so this delay allows the index to become queryable
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (drop) {
            store.removeAll();
        }

        return store;
    }

    private String buildUri(RunContext runContext) throws IllegalVariableEvaluationException {

        // Format: mongodb+srv://[username:password@]host[/[database][?options]]

        var username = runContext.render(this.username).as(String.class).orElse(null);
        var password = runContext.render(this.password).as(String.class).orElse(null);
        var host = runContext.render(this.host).as(String.class).orElseThrow();
        var database = runContext.render(this.database).as(String.class).orElseThrow();
        var options = runContext.render(this.options).asMap(String.class, Object.class);

        boolean isAtlas = host.endsWith(".mongodb.net");
        var scheme = isAtlas ? "mongodb+srv" : "mongodb"; // mongodb here is just for test purposes

        return scheme + "://" +
            (username != null && password != null ? username + ":" + password + "@" : "") +
            host + "/" +
            database + // optional in the connection string but still required in MongoDbEmbeddingStore code :shrug:
            toMongoOptionsQueryString(options);
    }

    private String toMongoOptionsQueryString(Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }

        return options.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&", "?", ""));
    }
}
