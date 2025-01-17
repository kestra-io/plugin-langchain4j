package io.kestra.plugin.langchain4j;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor

public abstract class AbstractTextClassification extends Task implements RunnableTask<AbstractTextClassification.Output> {

    @Schema(
        title = "Text prompt",
        description = "The input text to classify"
    )
    @NotNull
    protected Property<String> prompt;

    @Schema(
        title = "Classes",
        description = "The list of possible classes for classification"
    )
    @NotNull
    protected Property<List<String>> classes;


    @Override
    public AbstractTextClassification.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render inputs
        String renderedPrompt = runContext.render(prompt).as(String.class).orElseThrow();
        List<String> renderedClasses = runContext.render(classes).asList(String.class);

        // Log inputs
        logger.info("Prompt: {}", renderedPrompt);
        logger.info("Classes: {}", renderedClasses);

        // Instantiate the model
        ChatLanguageModel model = createModel(runContext);

        // Generate classification-specific prompt
        String classificationPrompt = renderedPrompt +
            "\nChoose only one of the following classes by typing just the exact class name: " + renderedClasses;

        // Generate the classification result
        String generatedClass = model.generate(classificationPrompt).trim();
        logger.info("Generated Class: {}", generatedClass);

        return Output.builder()
            .label(generatedClass)
            .build();
    }

    /**
     * Subclasses implement this to provide the specific language model.
     */
    protected abstract ChatLanguageModel createModel(RunContext runContext) throws Exception;

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Predicted label",
            description = "The label predicted by the model"
        )
        private final String label;
    }
}
