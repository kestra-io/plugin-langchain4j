package io.kestra.plugin.langchain4j;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.model.ChatModelFactory;
import io.kestra.plugin.langchain4j.model.Provider;
import io.kestra.plugin.langchain4j.model.ProviderConfig;
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
public class TextCompletion extends Task implements RunnableTask<TextCompletion.Output> {

    @Schema(title = "Text prompt", description = "The input prompt for the language model")
    @NotNull
    protected Property<String> prompt;

    @Schema(title = "Provider Configuration", description = "Configuration for the provider (e.g., API key, model name, endpoint)")
    @NotNull
    private ProviderConfig provider;

    @Override
    public TextCompletion.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render input properties
        String renderedPrompt = runContext.render(prompt).as(String.class).orElseThrow();
        Provider renderedProviderType = provider.getType();
        String renderedModelName =runContext.render(provider.getModelName()).as(String.class).orElse(null);
        String renderedApiKey =  runContext.render(provider.getApiKey()).as(String.class).orElse(null);
        String renderedEndpoint = runContext.render(provider.getEndpoint()).as(String.class).orElse(null);

        // Get the model
        ChatLanguageModel model = ChatModelFactory.createModel(renderedProviderType, renderedApiKey, renderedModelName, renderedEndpoint);

        String completion = model.generate(renderedPrompt);
        logger.info("Generated Completion: {}", completion);

        return Output.builder()
            .completion(completion)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Generated text completion", description = "The result of the text completion")
        private final String completion;
    }
}
