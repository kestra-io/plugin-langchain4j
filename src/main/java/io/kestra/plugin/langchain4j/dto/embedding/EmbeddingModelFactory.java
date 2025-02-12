package io.kestra.plugin.langchain4j.dto.embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;

public class EmbeddingModelFactory {

    public static EmbeddingModel createModel(ProviderEmbedding provider, String apiKey, String modelName, String endpoint, String projectId, String location) {
        return switch (provider) {
            case OPENAI -> OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
            case OLLAMA -> OllamaEmbeddingModel.builder()
                .baseUrl(endpoint)
                .modelName(modelName)
                .build();
            case GOOGLE_VERTEX_AI -> VertexAiEmbeddingModel.builder()
                .project(projectId)
                .location(location)
                .modelName(modelName)
                .build();
        };
    }
}