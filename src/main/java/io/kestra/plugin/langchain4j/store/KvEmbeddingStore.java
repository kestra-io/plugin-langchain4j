package io.kestra.plugin.langchain4j.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.exceptions.ResourceExpiredException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.storages.kv.KVEntry;
import io.kestra.core.storages.kv.KVValue;
import io.kestra.core.storages.kv.KVValueAndMetadata;
import io.kestra.plugin.langchain4j.domain.EmbeddingStoreProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
@SuperBuilder
@NoArgsConstructor
@Plugin
@JsonDeserialize
public class KvEmbeddingStore extends EmbeddingStoreProvider {
    @JsonIgnore
    private transient InMemoryEmbeddingStore<TextSegment> embeddingStore;

    @Builder.Default
    private Property<String> kvName = new Property<>("{{flow.id}}-embedding-store");

    @Override
    public EmbeddingStore<TextSegment> embeddingStore(RunContext runContext) throws IOException, IllegalVariableEvaluationException {
        String key = runContext.render(kvName).as(String.class).orElseThrow();
        Optional<KVEntry> kvEntry = runContext.namespaceKv(runContext.flowInfo().namespace()).get(key);
        if (kvEntry.isPresent()) {
            try {
                Optional<KVValue> value = runContext.namespaceKv(runContext.flowInfo().namespace()).getValue(kvEntry.get().key());
                embeddingStore = InMemoryEmbeddingStore.fromJson(Objects.requireNonNull(value.orElseThrow().value()).toString());
            } catch (ResourceExpiredException ree) {
                // Should not happen as we didn't set any expiry on the KV
                throw new IOException(ree);
            }
        } else {
            embeddingStore = new InMemoryEmbeddingStore<>();
        }

        return embeddingStore;
    }

    @Override
    public Map<String, Object> outputs(RunContext runContext) throws IOException, IllegalVariableEvaluationException {
        String key = runContext.render(kvName).as(String.class).orElseThrow();
        String storeContent = embeddingStore.serializeToJson();

        KVValueAndMetadata kvValueAndMetadata = new KVValueAndMetadata(null, storeContent);
        runContext.namespaceKv(runContext.flowInfo().namespace()).put(key, kvValueAndMetadata);

        return Map.of("kvName", key);
    }
}
