package io.kestra.plugin;

import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiImageModelName;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;

import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain.AbstractImageGeneration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;


@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "LangChain4j OpenAI Image generation Task",
    description = "Image generation using LangChain4j"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Image generation Example",
            code = {
                "prompt: \"Donald Duck in New York, cartoon style\"",
                "model: \"dall-e-3\""
            }
        )
    }
)
public class OpenAIImageGeneration extends AbstractImageGeneration {

    @Schema(
        title = "OpenAi model",
        description = "OpenAi image generation model name"
    )
    @NotNull
    private Property<OpenAiImageModelName> openAiImageModelName = Property.of(OpenAiImageModelName.DALL_E_3);


    @Schema(
        title = "API Key",
        description = "API key for the image generation model"
    )
    @NotNull
    protected Property<String> apikey;


    @Override
    protected ImageModel createModel(RunContext runContext, String apiUrl) throws IllegalVariableEvaluationException {
        OpenAiImageModelName renderedModelName = runContext.render(openAiImageModelName).as(OpenAiImageModelName.class)
            .orElseThrow();
        String renderedApiKey = runContext.render(apikey).as(String.class)
            .orElseThrow();

        return OpenAiImageModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderedModelName)
            .baseUrl(apiUrl != null ? apiUrl : "https://api.openai.com/v1")
            .build();
    }

}
