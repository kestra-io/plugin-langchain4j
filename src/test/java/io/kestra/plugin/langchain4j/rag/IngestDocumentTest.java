package io.kestra.plugin.langchain4j.rag;

import com.fasterxml.jackson.databind.JsonNode;
import io.kestra.core.exceptions.ResourceExpiredException;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.core.storages.kv.KVEntry;
import io.kestra.core.storages.kv.KVStore;
import io.kestra.core.storages.kv.KVValue;
import io.kestra.plugin.langchain4j.ContainerTest;
import io.kestra.plugin.langchain4j.provider.Ollama;
import io.kestra.plugin.langchain4j.embeddings.KestraKVStore;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
class IngestDocumentTest extends ContainerTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void inlineDocuments() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "flow", Map.of("id", "flow", "namespace", "namespace")
        ));

        var task = IngestDocument.builder()
            .provider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(new Property<>("{{ modelName }}"))
                    .endpoint(new Property<>("{{ endpoint }}"))
                    .build()
            )
            .embeddings(
                KestraKVStore.builder().build()
            )
            .drop(Property.of(true))
            .fromDocuments(List.of(IngestDocument.InlineDocument.builder().content(Property.of("I'm Loïc")).build()))
            .build();

        IngestDocument.Output output = task.run(runContext);
        assertThat(output.getIngestedDocuments()).isEqualTo(1);

        String kvKey = (String) output.getEmbeddingStoreOutputs().get("kvName");
        KVStore kvStore = runContext.namespaceKv(runContext.flowInfo().namespace());
        assertKvSore(kvStore, kvKey, 1);
    }

    @Test
    void internalStorageURIs() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "flow", Map.of("id", "flow", "namespace", "namespace")
        ));

        Path path = runContext.workingDir().createFile("document.txt");
        Files.write(path, "I'm Loïc".getBytes());
        URI uri = runContext.storage().putFile(path.toFile());

        var task = IngestDocument.builder()
            .provider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(new Property<>("{{ modelName }}"))
                    .endpoint(new Property<>("{{ endpoint }}"))
                    .build()
            )
            .embeddings(
                KestraKVStore.builder().build()
            )
            .drop(Property.of(true))
            .fromInternalURIs(Property.of(List.of(uri.toString())))
            .build();

        IngestDocument.Output output = task.run(runContext);
        assertThat(output.getIngestedDocuments()).isEqualTo(1);

        String kvKey = (String) output.getEmbeddingStoreOutputs().get("kvName");
        KVStore kvStore = runContext.namespaceKv(runContext.flowInfo().namespace());
        assertKvSore(kvStore, kvKey, 1);
    }

    @Test
    void workingDirectoryPath() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "flow", Map.of("id", "flow", "namespace", "namespace")
        ));

        Path path1 = runContext.workingDir().createFile("ingest/document1.txt");
        Files.write(path1, "I'm Loïc".getBytes());
        Path path2 = runContext.workingDir().createFile("ingest/document2.txt");
        Files.write(path2, "I live in Lille".getBytes());

        var task = IngestDocument.builder()
            .provider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(new Property<>("{{ modelName }}"))
                    .endpoint(new Property<>("{{ endpoint }}"))
                    .build()
            )
            .embeddings(
                KestraKVStore.builder().build()
            )
            .drop(Property.of(true))
            .fromPath(Property.of("ingest"))
            .build();

        IngestDocument.Output output = task.run(runContext);
        assertThat(output.getIngestedDocuments()).isEqualTo(2);

        String kvKey = (String) output.getEmbeddingStoreOutputs().get("kvName");
        KVStore kvStore = runContext.namespaceKv(runContext.flowInfo().namespace());
        assertKvSore(kvStore, kvKey, 2);
    }

    @Test
    void externalURLs() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "flow", Map.of("id", "flow", "namespace", "namespace")
        ));

        var task = IngestDocument.builder()
            .provider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(new Property<>("{{ modelName }}"))
                    .endpoint(new Property<>("{{ endpoint }}"))
                    .build()
            )
            .embeddings(
                KestraKVStore.builder().build()
            )
            .drop(Property.of(true))
            .fromExternalURLs(Property.of(List.of("https://dummyjson.com/products/1", "https://dummyjson.com/products/2")))
            .build();

        IngestDocument.Output output = task.run(runContext);
        assertThat(output.getIngestedDocuments()).isEqualTo(2);

        String kvKey = (String) output.getEmbeddingStoreOutputs().get("kvName");
        KVStore kvStore = runContext.namespaceKv(runContext.flowInfo().namespace());
        assertKvSore(kvStore, kvKey, 2);
    }

    private void assertKvSore(KVStore kvStore, String kvKey, int nbDocuments) throws IOException, ResourceExpiredException {
        Optional<KVEntry> kvEntry = kvStore.get(kvKey);
        assertThat(kvEntry.isPresent()).isTrue();
        Optional<KVValue> kvValue = kvStore.getValue(kvEntry.get().key());
        assertThat(kvValue.isPresent()).isTrue();
        assertThat(kvValue.get().value()).isNotNull();
        String value  = kvValue.get().value().toString();
        JsonNode jsonNode = JacksonMapper.ofJson().readTree(value);
        assertThat(jsonNode.get("entries")).isNotNull();
        assertThat(jsonNode.get("entries").size()).isEqualTo(nbDocuments);
    }
}