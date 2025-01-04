package io.kestra.plugin.langchain;

import dev.langchain4j.model.openai.OpenAiChatModel;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "LangChain4j Text Completion Task",
    description = "Generates text completion using LangChain4j"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Text Completion Example",
            code = {
                "prompt: \"What is the capital of France?\"",
                "model: \"gpt-4\""
            }
        )
    }
)
public class LangChainTextCompletion extends Task implements RunnableTask<LangChainTextCompletion.Output> {
    @Schema(
        title = "Text prompt",
        description = "The input prompt for the language model"
    )
    private Property<String> prompt;

    private OpenAiChatModel openAiChatModel;

    @Override
    public LangChainTextCompletion.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render the input prompt
        String renderedPrompt = runContext.render(prompt).as(String.class).orElse("");

        logger.info("Prompt: {}", renderedPrompt);

        // Generate text completion
        String answer = openAiChatModel.generate(renderedPrompt);
        logger.info("Generated Completion: {}", answer);

        return Output.builder()
            .completion(answer)
            .build();
    }

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
