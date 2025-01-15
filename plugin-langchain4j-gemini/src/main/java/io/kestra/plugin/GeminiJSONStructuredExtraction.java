package io.kestra.plugin;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.enums.GeminiModel;
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
            code = {
                "fields: [\"name\", \"date\"]",
                "prompt: \"Hello, my name is John\"",
                "model: \"gemini-1.5-flash\""
            }
        )
    }
)
public class GeminiJSONStructuredExtraction extends AbstractJSONStructuredExtraction {


    @Schema(
        title = "Gemini Model",
        description = "Gemini-specific model configuration"
    )
    @NotNull
    private Property<GeminiModel> modelName= Property.of(GeminiModel.GEMINI_1_5_FLASH);

    @Schema(
        title = "API Key",
        description = "API key for the language model"
    )
    @NotNull
    protected Property<String> apikey;

    @Override
    protected ChatLanguageModel createModel(RunContext runContext) throws IllegalVariableEvaluationException {
        GeminiModel renderedModelName = runContext.render(modelName).as(GeminiModel.class)
            .orElseThrow();
        String renderedApiKey = runContext.render(apikey).as(String.class)
            .orElseThrow();

        return GoogleAiGeminiChatModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderedModelName.getName())
            .build();
    }
}
