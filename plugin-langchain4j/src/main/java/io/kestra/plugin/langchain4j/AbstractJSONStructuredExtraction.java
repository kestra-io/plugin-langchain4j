package io.kestra.plugin.langchain4j;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
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
public abstract class AbstractJSONStructuredExtraction extends Task implements RunnableTask<AbstractJSONStructuredExtraction.Output> {

    @Schema(
        title = "Text prompt",
        description = "The input prompt for the language model"
    )
    @NotNull
    protected Property<String> prompt;

    @Schema(
        title = "Json fields",
        description = "The list of fields to be extracted"
    )
    @NotNull
    protected Property<List<String>> jsonFields;

    @Schema(
        title = "Name of the schema",
        description = "Schema name of the structured extraction"
    )
    @NotNull
    protected Property<String> schemaName;


    @Override
    public AbstractJSONStructuredExtraction.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render inputs
        String renderedPrompt = runContext.render(prompt).as(String.class).orElseThrow();
        String renderedSchemaName = runContext.render(schemaName).as(String.class).orElseThrow();
        List<String> renderedFields = Property.asList(jsonFields, runContext, String.class);

        // Build JSON schema
        ResponseFormat responseFormat = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(JsonSchema.builder()
                .name(renderedSchemaName)
                .rootElement(buildDynamicSchema(renderedFields))
                .build())
            .build();

        // Build request
        ChatRequest chatRequest = ChatRequest.builder()
            .responseFormat(responseFormat)
            .messages(UserMessage.from(renderedPrompt))
            .build();

        // Create model instance
        ChatLanguageModel model = createModel(runContext);

        // Generate response
        ChatResponse answer = model.chat(chatRequest);
        logger.info("Generation Complete!");

        return Output.builder()
            .result(answer.aiMessage().text())
            .build();
    }

    /**
     * Subclasses implement this to create the specific model instance.
     */
    protected abstract ChatLanguageModel createModel(RunContext runContext) throws Exception;

    public static JsonObjectSchema buildDynamicSchema(List<String> fields) {
        JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
        fields.forEach(schemaBuilder::addStringProperty);
        schemaBuilder.required(fields);
        return schemaBuilder.build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Generated text completion",
            description = "The result of the text completion"
        )
        private final String result;
    }
}
