package io.kestra.plugin.langchain4j;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.dto.text.ChatModelFactory;
import io.kestra.plugin.langchain4j.dto.text.Provider;
import io.kestra.plugin.langchain4j.dto.text.ProviderConfig;
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
@Plugin(
    examples = {
        @Example(
            title = "Text Completion using OpenAI",
            full = true,
            code = {
                """
                id: openai_text_completion
                namespace: company.team
                task:
                    id: text_completion
                    prompt: What is the capital of France?
                    provider:
                        type: OPEN_AI
                        apiKey: your_openai_api_key
                        modelName: gpt-4o
                """
            }
        ),
        @Example(
            title = "Text Completion using Ollama",
            full = true,
            code = {
                """
                id: ollama_text_completion
                namespace: company.team
                task:
                    id: text_completion
                    prompt: What is the capital of France?
                    provider:
                        type: OLLAMA
                        modelName: llama3
                        endpoint: http://localhost:11434
                """
            }
        ),
        @Example(
            title = "Text Completion using Gemini",
            full = true,
            code = {
                """
                id: gemini_text_completion
                namespace: company.team
                task:
                    id: text_completion
                    prompt: Summarize the history of the Eiffel Tower
                    provider:
                        type: GOOGLE_GEMINI
                        apiKey: your_gemini_api_key
                        modelName: gemini-1.5-flash
                """
            }
        )
    }
)
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
