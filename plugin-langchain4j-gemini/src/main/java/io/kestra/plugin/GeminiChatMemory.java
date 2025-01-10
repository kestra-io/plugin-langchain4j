package io.kestra.plugin;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.enums.EGeminiModel;
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
    title = "Gemini Chat Memory Task",
    description = "Handles chat interactions with memory using Gemini models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Chat Memory Example",
            code = {
                "userMessage: \"Hello, my name is John\"",
                "apikey: \"your-gemini-api-key\"",
                "modelName: \"gemini-1.5\""
            }
        )
    }
)
public class GeminiChatMemory extends AbstractChatMemory {

    @Schema(
        title = "Gemini Model Name",
        description = "Name of the Gemini model to use"
    )
    @NotNull
    private Property<EGeminiModel> modelName= Property.of(EGeminiModel.GEMINI_1_5_FLASH);

    @Override
    protected ChatLanguageModel createModel(RunContext runContext, String apiKey) throws IllegalVariableEvaluationException {

        return GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName(renderModelName(runContext).getName())
            .logRequestsAndResponses(true)
            .build();
    }

    private EGeminiModel renderModelName (RunContext runContext) throws IllegalVariableEvaluationException {
        return runContext.render(modelName).as(EGeminiModel.class).orElseThrow();
    }
}

