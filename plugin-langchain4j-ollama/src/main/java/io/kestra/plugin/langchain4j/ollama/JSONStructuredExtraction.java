package io.kestra.plugin.langchain4j.ollama;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.AbstractJSONStructuredExtraction;
import io.kestra.plugin.langchain4j.ollama.enums.EOllamaModel;
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
    title = "Ollama JSON Structured Extraction Task",
    description = "Generates JSON structured extraction using Ollama models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Structured Extraction Example",
            code = {
                "fields: [\"location\", \"temperature\"]",
                "prompt: \"In Paris, it's 20 degrees Celsius.\"",
                "ollamaEndpoint: \"http://localhost:11434\""
            }
        )
    }
)
public class JSONStructuredExtraction extends AbstractJSONStructuredExtraction {

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
    private Property<EOllamaModel> ollamaModelName = Property.of(EOllamaModel.OLLAMA3_3);

    @Override
    protected ChatLanguageModel createModel(RunContext runContext) throws Exception {
        String renderedEndpoint = runContext.render(ollamaEndpoint).as(String.class).orElseThrow();
        EOllamaModel renderedModelName = runContext.render(ollamaModelName).as(EOllamaModel.class).orElseThrow();

        return OllamaChatModel.builder()
            .baseUrl(renderedEndpoint)
            .modelName(renderedModelName.getName())
            .logRequests(true)
            .logResponses(true)
            .build();
    }
}
