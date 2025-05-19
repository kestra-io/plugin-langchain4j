package io.kestra.plugin.langchain4j.provider;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.vertexai.VertexAiImageModel;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.ModelProvider;
import io.kestra.plugin.langchain4j.domain.ChatConfiguration;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(
    title = "Google VertexAI Model Provider"
)
public class GoogleVertexAI extends ModelProvider {
    @Schema(title = "Endpoint URL")
    @NotNull
    private Property<String> endpoint;

    @Schema(title = "Project location")
    @NotNull
    private Property<String> location;

    @Schema(title = "Project ID")
    @NotNull
    private Property<String> project;

    @Override
    public ChatModel chatModel(RunContext runContext, ChatConfiguration configuration) throws IllegalVariableEvaluationException {
        if (this.endpoint != null) {
            throw new IllegalArgumentException("The `endpoint` property cannot be used for the Chat Model which uses Gemini only.");
        }

        return VertexAiGeminiChatModel.builder()
            .modelName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
            .location(runContext.render(this.location).as(String.class).orElseThrow())
            .project(runContext.render(this.project).as(String.class).orElseThrow())
            .temperature(runContext.render(configuration.getTemperature()).as(Double.class).map(d -> d.floatValue()).orElse(null))
            .topK(runContext.render(configuration.getTopK()).as(Integer.class).orElse(null))
            .topP(runContext.render(configuration.getTopP()).as(Double.class).map(d -> d.floatValue()).orElse(null))
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
