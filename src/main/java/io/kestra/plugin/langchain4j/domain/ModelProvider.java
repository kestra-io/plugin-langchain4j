package io.kestra.plugin.langchain4j.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.plugins.AdditionalPlugin;
import io.kestra.core.plugins.serdes.PluginDeserializer;
import io.kestra.core.runners.RunContext;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Plugin
@SuperBuilder(toBuilder = true)
@Getter
@NoArgsConstructor
// IMPORTANT: The abstract plugin base class must define using the PluginDeserializer,
// AND concrete subclasses must be annotated by @JsonDeserialize() to avoid StackOverflow.
@JsonDeserialize(using = PluginDeserializer.class)
public abstract class ModelProvider extends AdditionalPlugin {
    @NotNull
    private Property<String> modelName;

    public abstract ChatLanguageModel chatLanguageModel(RunContext runContext, ChatConfiguration configuration) throws IllegalVariableEvaluationException;

    public abstract ImageModel imageModel(RunContext runContext) throws IllegalVariableEvaluationException;

    public abstract EmbeddingModel embeddingModel(RunContext runContext) throws IllegalVariableEvaluationException;
}
