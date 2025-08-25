package io.kestra.plugin.ai.domain;

import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.ai.tool.internal.JsonObjectSchemaTranslator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ChatConfiguration {
    @Schema(title = "Temperature")
    private Property<Double> temperature;

    @Schema(title = "topK")
    private Property<Integer> topK;

    @Schema(title = "topP")
    private Property<Double> topP;

    @Schema(title = "seed")
    private Property<Integer> seed;

    @Schema(
        title = "Whether to log LLM requests",
        description = "Log will be sent to the server log in DEBUG."
    )
    private Property<Boolean> logRequests;

    @Schema(
        title = "Whether to log LLM responses",
        description = "Log will be sent to the server log in DEBUG."
    )
    private Property<Boolean> logResponses;

    @Schema(
        title = "The LLM response format, 'TEXT' by default",
        description = """
            By default, LLM responses are non-structured texts.
            You can ask for a different response format but be aware that not all LLM providers support setting the response format,
            and the ones that support it may not support it in conjunction with tools.
            If you specify a JSON schema, the name of the response will be `output`, you would be able to refer it in your prompt."""
    )
    private ResponseFormat responseFormat;

    public dev.langchain4j.model.chat.request.ResponseFormat computeResponseFormat(RunContext runContext) throws IllegalVariableEvaluationException {
        if (responseFormat == null) {
            return dev.langchain4j.model.chat.request.ResponseFormat.TEXT;
        }

        return responseFormat.to(runContext);
    }

    public static ChatConfiguration empty() {
        return ChatConfiguration.builder().build();
    }

    @Getter
    @Builder
    public static class ResponseFormat {
        @Schema(title = "The response format type")
        @NotNull
        @Builder.Default
        private Property<ResponseFormatType> type =  Property.ofValue(ResponseFormatType.TEXT);

        @Schema(
            title = "The JSON schema for structured output, set it only for response format type `JSON`",
            description = "This should be a valid JSON Schema, see https://json-schema.org/specification."
        )
        private Property<Map<String, Object>> jsonSchema;

        @Schema(title = "A description for the JSON schema")
        private Property<String> jsonSchemaDescription;

        dev.langchain4j.model.chat.request.ResponseFormat to(RunContext runContext) throws IllegalVariableEvaluationException {
            var responseFormatType = runContext.render(type).as(ResponseFormatType.class).orElse(ResponseFormatType.TEXT);
            if (responseFormatType == ResponseFormatType.TEXT && jsonSchema != null) {
                throw new IllegalArgumentException("`jsonSchema` property is only allowed when `type` is `JSON`");
            }

            JsonSchema langchain4jJsonSchema = null;
            if (jsonSchema != null) {
                JsonObjectSchema jsonObjectSchema = JsonObjectSchemaTranslator.fromOpenAPISchema(runContext.render(jsonSchema).asMap(String.class, Object.class), runContext.render(jsonSchemaDescription).as(String.class).orElse(null));
                langchain4jJsonSchema = JsonSchema.builder().name("output").rootElement(jsonObjectSchema).build();
            }
            return dev.langchain4j.model.chat.request.ResponseFormat.builder()
                .type(runContext.render(type).as(ResponseFormatType.class).orElse(ResponseFormatType.TEXT))
                .jsonSchema(langchain4jJsonSchema)
                .build();
        }
    }
}
