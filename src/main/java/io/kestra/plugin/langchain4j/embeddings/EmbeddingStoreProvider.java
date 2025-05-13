package io.kestra.plugin.langchain4j.stores;

import dev.langchain4j.store.embedding.EmbeddingStore;

public interface EmbeddingStoreProvider {
    EmbeddingStore<String> get();
}
