package io.kestra.plugin.langchain4j.gemini;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.AbstractChatCompletion;
import io.kestra.plugin.langchain4j.gemini.enums.GeminiModel;
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
            full = true,
            code = {
                "Messages: [",
                "  { \"type\": \"USER\", \"content\": \"Hello, my name is John\" },",
                "  { \"type\": \"AI\", \"content\": \"Welcome John, how can I assist you today?\" },",
                "  { \"type\": \"USER\", \"content\": \"I need help with my account\" }",
                "]",
                "apiKey: \"your-gemini-api-key\"",
                "modelName: \"GEMINI_1_5_FLASH\""
            }
        )
    }
)
public class ChatCompletion extends AbstractChatCompletion {

    @Schema(
        title = "Gemini Model Name",
        description = "Name of the Gemini model to use"
    )
    @NotNull
    private Property<GeminiModel> modelName= Property.of(GeminiModel.GEMINI_1_5_FLASH);

    @Override
    protected ChatLanguageModel createModel(RunContext runContext, String apiKey) throws IllegalVariableEvaluationException {

        return GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName(renderModelName(runContext).getName())
            .logRequestsAndResponses(true)
            .build();
    }

    private GeminiModel renderModelName (RunContext runContext) throws IllegalVariableEvaluationException {
        return runContext.render(modelName).as(GeminiModel.class).orElseThrow();
    }
}

