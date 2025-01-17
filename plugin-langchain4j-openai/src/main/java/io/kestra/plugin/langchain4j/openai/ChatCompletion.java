package io.kestra.plugin.langchain4j.openai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.AbstractChatCompletion;
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
    title = "OpenAI Chat Completion Task",
    description = "Handles chat interactions using OpenAI models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Chat Memory Example",
            code = {
                "userMessage: \"Hello, my name is John\"",
                "apikey: \"your-openai-api-key\"",
                "modelName: \"gpt-4\""
            }
        )
    }
)
public class ChatCompletion extends AbstractChatCompletion {

    @Schema(
        title = "OpenAI Model Name",
        description = "OpenAI model name"
    )
    @NotNull
    private Property<OpenAiChatModelName> modelName;

    @Override
    protected ChatLanguageModel createModel(RunContext runContext, String apiKey) throws Exception {

        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(renderModelName(runContext))
            .build();
    }

    private OpenAiChatModelName renderModelName (RunContext runContext) throws IllegalVariableEvaluationException {
        return runContext.render(modelName).as(OpenAiChatModelName.class).orElseThrow();
    }
}
