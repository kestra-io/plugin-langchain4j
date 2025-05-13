package io.kestra.plugin.langchain4j.models;

import dev.langchain4j.model.embedding.EmbeddingModel;

public interface ModelProvider {
    EmbeddingModel getEmbeddingModel();
}
