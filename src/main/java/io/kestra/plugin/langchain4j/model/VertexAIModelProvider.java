package io.kestra.plugin.langchain4j.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiImageModel;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.ModelProvider;
import io.kestra.plugin.langchain4j.domain.ChatConfiguration;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Plugin(beta = true)
@JsonDeserialize
public class VertexAIModelProvider extends ModelProvider {
    @NotNull
    private Property<String> endpoint;

    @NotNull
    private Property<String> location;

    @NotNull
    private Property<String> project;

    @Override
    public ChatLanguageModel chatLanguageModel(RunContext runContext, ChatConfiguration configuration) throws IllegalVariableEvaluationException {
        return VertexAiChatModel.builder()
            .modelName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
            .endpoint(runContext.render(this.endpoint).as(String.class).orElseThrow())
            .location(runContext.render(this.location).as(String.class).orElseThrow())
            .project(runContext.render(this.project).as(String.class).orElseThrow())
            .temperature(runContext.render(configuration.getTemperature()).as(Double.class).orElse(null))
            .topK(runContext.render(configuration.getTopK()).as(Integer.class).orElse(null))
            .topP(runContext.render(configuration.getTopP()).as(Double.class).orElse(null))
            .build();
    }

    @Override
    public ImageModel imageModel(RunContext runContext) throws IllegalVariableEvaluationException {
        return VertexAiImageModel.builder()
            .modelName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
            .endpoint(runContext.render(this.endpoint).as(String.class).orElse(null))
            .location(runContext.render(this.location).as(String.class).orElse(null))
            .project(runContext.render(this.project).as(String.class).orElseThrow())
            .build();
    }

    @Override
    public EmbeddingModel embeddingModel(RunContext runContext) throws IllegalVariableEvaluationException {
        return VertexAiEmbeddingModel.builder()
            .modelName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
            .endpoint(runContext.render(this.endpoint).as(String.class).orElse(null))
            .location(runContext.render(this.location).as(String.class).orElse(null))
            .project(runContext.render(this.project).as(String.class).orElseThrow())
            .build();
    }
}
