package io.kestra.plugin;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
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
    title = "OpenAI Text Classification Task",
    description = "Classifies text using OpenAI models"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Classification Example",
            code = {
                "prompt: \"Is 'This is a joke' a good joke?\"",
                "classes: [\"true\", \"false\"]",
                "model: \"gpt-4\""
            }
        )
    }
)
public class OpenAIClassification extends AbstractTextClassification {

    @Schema(
        title = "OpenAI Model Name",
        description = "The OpenAI model to use"
    )
    @NotNull
    private Property<OpenAiChatModelName> openAiChatModelName;

    @Schema(
        title = "API Key",
        description = "API key for the language model"
    )
    @NotNull
    protected Property<String> apikey;

    @Override
    protected ChatLanguageModel createModel(RunContext runContext) throws Exception {
        OpenAiChatModelName renderedModelName = runContext.render(openAiChatModelName).as(OpenAiChatModelName.class)
            .orElseThrow();
        String renderedApiKey = runContext.render(apikey).as(String.class)
            .orElseThrow();

        return OpenAiChatModel.builder()
            .apiKey(renderedApiKey)
            .modelName(renderedModelName)
            .build();
    }
}
