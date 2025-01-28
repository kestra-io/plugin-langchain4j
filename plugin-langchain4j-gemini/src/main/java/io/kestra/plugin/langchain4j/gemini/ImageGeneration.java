package io.kestra.plugin.langchain4j.gemini;

import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.vertexai.VertexAiImageModel;
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
    title = "Google Gemini Image Generation Task",
    description = "Generates images using Google Gemini models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Image Generation Example",
            full = true,
            code = {
                "prompt: \"A beautiful sunset over mountains\"",
                "apiKey: \"gemini-api-key\"",
                "modelName: \"imagegeneration@002\"",
                "projectId: \"your-google-project-id\"",
                "location: \"us-central1\"",
                "apiUrl: \"https://us-central1-aiplatform.googleapis.com/v1/\"",
            }
        )
    }
)
public class ImageGeneration extends AbstractImageGeneration {

    @Schema(
        title = "Gemini Model",
        description = "the name of the image model (<code>imagegeneration@002</code> or <code>imagegeneration@005</code>)"
    )
    @NotNull
    private Property<String> modelName;

    @Schema(
        title = "GCP project ID",
        description = "Google provider project id"
    )
    @NotNull
    private Property<String> projectId;

    @Schema(
        title = "Cloud region",
        description = "the cloud region (eg. <code>us-central1</code>)"
    )
    @NotNull
    private Property<String> location;

    /**
     *
     * @param endpoint the base URL of the API (eg. <code>https://us-central1-aiplatform.googleapis.com/v1/</code>)
     */
    @Override
    protected ImageModel createModel(RunContext runContext, String endpoint)
        throws IllegalVariableEvaluationException {
        String renderedModelName = runContext.render(modelName).as(String.class)
            .orElseThrow();
        String renderedProjectId = runContext.render(projectId).as(String.class)
            .orElseThrow();
        String renderedLocation = runContext.render(location).as(String.class)
            .orElseThrow();

        return VertexAiImageModel.builder()
            .endpoint(endpoint)
            .project(renderedProjectId)
            .location(renderedLocation)
            .modelName(renderedModelName)
            .withPersisting()
            .build();
    }
}