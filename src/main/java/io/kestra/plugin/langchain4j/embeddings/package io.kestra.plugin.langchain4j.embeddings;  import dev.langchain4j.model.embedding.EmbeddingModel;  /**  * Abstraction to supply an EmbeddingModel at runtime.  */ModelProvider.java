package io.kestra.plugin.langchain4j.embeddings;

import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * Abstraction to supply an EmbeddingModel at runtime.
 */
public interface ModelProvider {
    EmbeddingModel get();
}
