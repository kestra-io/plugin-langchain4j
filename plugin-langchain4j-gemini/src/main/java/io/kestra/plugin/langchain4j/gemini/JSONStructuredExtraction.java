package io.kestra.plugin.langchain4j.gemini;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.AbstractJSONStructuredExtraction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Google Gemini JSON Structured Extraction Task",
    description = "Generates JSON structured extraction using Gemini models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Structured Extraction Example",
            full = true,
            code = {
                """
                id: gemini_json_structured_extraction
                namespace: company.team
                task:
                    id: json_structured_extraction
                    prompt: Hello, my name is John, I live in Paris
                    jsonFields: [name, City]
                    schemaName: Person
                    apiKey: your-gemini-api-key
                    modelName: gemini-1.5-flash
                """
            }
        )
    }
)

public class JSONStructuredExtraction extends AbstractJSONStructuredExtraction {


    @Schema(
        title = "Gemini Model",
        description = "Gemini-specific model configuration"
    )
    @NotNull
    private Property<String> modelName;

    @Schema(
        title = "API Key",
        description = "API key for the language model"
    )
    @NotNull
    protected Property<String> apiKey;

    @Override
    protected ChatLanguageModel createModel(RunContext runContext) throws IllegalVariableEvaluationException {
        String renderedModelName = runContext.render(modelName).as(String.class)
            .orElseThrow();
        String renderedApiKey = runContext.render(apiKey).as(String.class)
            .orElseThrow();

        return GoogleAiGeminiChatModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderedModelName)
            .build();
    }
}
