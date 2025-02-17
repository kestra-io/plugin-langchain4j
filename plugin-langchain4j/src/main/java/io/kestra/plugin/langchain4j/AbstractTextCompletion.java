package io.kestra.plugin.langchain4j;

import dev.langchain4j.model.chat.ChatLanguageModel;
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

public abstract class AbstractTextCompletion extends Task implements RunnableTask<AbstractTextCompletion.Output> {
    @Schema(
        title = "Text prompt",
        description = "The input prompt for the language model"
    )
    @NotNull
    protected Property<String> prompt;

    @Override
    public AbstractTextCompletion.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render input properties
        String renderedPrompt = runContext.render(prompt).as(String.class)
            .orElseThrow();

        // Instantiate model
        ChatLanguageModel model = createModel(runContext);

        // Generate text completion
        String completion = model.generate(renderedPrompt);
        logger.info("Generated Completion: {}", completion);


        return Output.builder()
            .completion(completion)
            .build();
    }

    /**
     * Subclasses implement this to provide the specific language model.
     */
    protected abstract ChatLanguageModel createModel(RunContext runContext) throws IllegalVariableEvaluationException;

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Generated text completion",
            description = "The result of the text completion"
        )
        private final String completion;
    }
}
