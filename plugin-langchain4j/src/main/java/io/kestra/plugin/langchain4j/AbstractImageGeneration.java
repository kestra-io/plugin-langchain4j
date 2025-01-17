package io.kestra.plugin.langchain4j;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractImageGeneration extends Task implements RunnableTask<AbstractImageGeneration.Output> {

    @Schema(
        title = "Text prompt",
        description = "The input prompt for the image generation model"
    )
    @NotNull
    protected Property<String> prompt;


    @Schema(
        title = "Base API URL",
        description = "The base URL for the API (optional)"
    )
    protected Property<String> apiUrl;

    @Override
    public AbstractImageGeneration.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render input properties
        String renderedPrompt = runContext.render(prompt).as(String.class)
            .orElseThrow();

        String renderedApiUrl = runContext.render(apiUrl).as(String.class).orElse("https://api.openai.com");

        // Instantiate Image model
        ImageModel model = createModel(runContext, renderedApiUrl);

        // Generate image
        Response<Image> generatedImage = model.generate(renderedPrompt);
        String imageUrl = generatedImage.content().url().toString();
        logger.info("Generated Image URL: {}", imageUrl);

        return Output.builder()
            .completion(imageUrl)
            .build();
    }

    /**
     * Subclasses implement this to provide the specific image generation model.
     */
    protected abstract ImageModel createModel(RunContext runContext, String apiUrl) throws IllegalVariableEvaluationException;

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Generated image URL",
            description = "The result of the image generation"
        )
        private final String completion;
    }
}
