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

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Text Classification Task", description = "Classifies text using AI models (OpenAI, Ollama, Gemini).")
public class Classification extends Task implements RunnableTask<Classification.Output> {

    @Schema(title = "Text prompt", description = "The input text to classify.")
    @NotNull
    private Property<String> prompt;

    @Schema(title = "Classification Options", description = "The list of possible classification categories.")
    @NotNull
    private Property<List<String>> classes;

    @Schema(title = "Provider Configuration", description = "Configuration for the provider (e.g., API key, model name, endpoint).")
    @NotNull
    private ProviderConfig provider;

    @Override
    public Classification.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render input properties
        String renderedPrompt = runContext.render(prompt).as(String.class).orElseThrow();
        List<String> renderedClasses = runContext.render(classes).asList(String.class);

        Provider renderedProviderType = provider.getType();
        String renderedModelName = runContext.render(provider.getModelName()).as(String.class).orElse(null);
        String renderedApiKey = runContext.render(provider.getApiKey()).as(String.class).orElse(null);
        String renderedEndpoint = runContext.render(provider.getEndpoint()).as(String.class).orElse(null);

        // Get the appropriate model from the factory
        ChatLanguageModel model = ChatModelFactory.createModel(renderedProviderType, renderedApiKey, renderedModelName, renderedEndpoint);

        String classificationPrompt = renderedPrompt +
            "\nRespond by only one of the following classes by typing just the exact class name: " + renderedClasses;

        // Perform text classification
        String classificationResponse = model.generate(classificationPrompt).trim();
        logger.info("Generated Classification: {}", classificationResponse);

        return Output.builder()
            .classification(classificationResponse)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Classification Result", description = "The classified category of the input text.")
        private final String classification;
    }
}
