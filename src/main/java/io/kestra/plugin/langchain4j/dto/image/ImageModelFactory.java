package io.kestra.plugin.langchain4j.dto.image;

import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.vertexai.VertexAiImageModel;

public class ImageModelFactory {

    public static ImageModel createModel(ProviderImage provider, String apiKey, String modelName, String projectId, String location, String endpoint, String publisher) {
        return switch (provider) {
            case OPENAI -> OpenAiImageModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(endpoint != null ? endpoint : "https://api.openai.com/v1")
                .build();
            case GOOGLE_VERTEX -> VertexAiImageModel.builder()
                .endpoint(endpoint)
                .location(location)
                .project(projectId)
                .publisher(publisher)
                .modelName(modelName)
                .build();
        };
    }
}