package io.kestra.plugin.langchain4j.openai;

import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiImageModelName;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;

import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.AbstractImageGeneration;
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
            title = "Image Generation Example",
            full = true,
            code = {
                """
                id: openai_image_generation
                namespace: company.team

                task:
                    id: image_generation
                    prompt: A beautiful sunset over mountains
                    apiKey: your_openai_api_key
                    modelName: dall-e-2
                """
            }
        )
    }
)

public class ImageGeneration extends AbstractImageGeneration {

    @Schema(
        title = "OpenAi model",
        description = "OpenAi image generation model name"
    )
    @NotNull
    private Property<String> modelName;


    @Schema(
        title = "API Key",
        description = "API key for the image generation model"
    )
    @NotNull
    protected Property<String> apiKey;


    @Override
    protected ImageModel createModel(RunContext runContext, String apiUrl) throws IllegalVariableEvaluationException {
        String renderedModelName = runContext.render(modelName).as(String.class)
            .orElseThrow();
        String renderedApiKey = runContext.render(apiKey).as(String.class)
            .orElseThrow();

        return OpenAiImageModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderedModelName)
            .baseUrl(apiUrl != null ? apiUrl : "https://api.openai.com/v1")
            .build();
    }

}
