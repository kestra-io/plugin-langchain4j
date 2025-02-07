package io.kestra.plugin.langchain4j.ollama;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.AbstractTextClassification;
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
    title = "Ollama Text Classification Task",
    description = "Classifies text using Ollama models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Classification Example",
            full = true,
            code = {
                """
                id: ollama_classification
                namespace: company.team

                task:
                    id: classification
                    prompt: Is 'This is a joke' a good joke?
                    classes:
                      - true
                      - false
                    modelName: llama3
                    ollamaEndpoint: http://localhost:11434
                """
            }
        )
    }
)

public class Classification extends AbstractTextClassification {

    @Schema(
        title = "Ollama Endpoint",
        description = "The base URL for the Ollama API"
    )
    @NotNull
    private Property<String> ollamaEndpoint;

    @Schema(
        title = "Ollama Model Name",
        description = "The Ollama model to use"
    )
    @NotNull
    private Property<String> modelName;

    @Override
    protected ChatLanguageModel createModel(RunContext runContext) throws Exception {
        String renderedUrl = runContext.render(ollamaEndpoint).as(String.class).orElseThrow();
        String renderedModelName= runContext.render(modelName).as(String.class).orElseThrow();

        return OllamaChatModel.builder()
            .baseUrl(renderedUrl)
            .modelName(renderedModelName)
            .logRequests(true)
            .logResponses(true)
            .build();
    }
}
