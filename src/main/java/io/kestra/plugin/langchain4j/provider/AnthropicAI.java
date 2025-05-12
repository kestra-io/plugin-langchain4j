package io.kestra.plugin.langchain4j.provider;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.ChatConfiguration;
import io.kestra.plugin.langchain4j.domain.ModelProvider;
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
    title = "Anthropic AI Model Provider"
)
public class AnthropicAI extends ModelProvider {
    @Schema(title = "API Key")
    @NotNull
    private Property<String> apiKey;

    @Override
    public ChatLanguageModel chatLanguageModel(RunContext runContext, ChatConfiguration configuration) throws IllegalVariableEvaluationException {
        return AnthropicChatModel.builder()
            .modelName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
            .apiKey(runContext.render(this.apiKey).as(String.class).orElseThrow())
            .temperature(runContext.render(configuration.getTemperature()).as(Double.class).orElse(null))
            .topK(runContext.render(configuration.getTopK()).as(Integer.class).orElse(null))
            .topP(runContext.render(configuration.getTopP()).as(Double.class).orElse(null))
            .logRequests(true)
            .logResponses(true)
            .cacheSystemMessages(true)
            .build();
    }

    @Override
    public ImageModel imageModel(RunContext runContext) {
        throw new UnsupportedOperationException("Anthropic didn't support image model");
    }

    @Override
    public EmbeddingModel embeddingModel(RunContext runContext) throws IllegalVariableEvaluationException {
        throw new UnsupportedOperationException("Anthropic doesn’t support embeddings");
    }

}
