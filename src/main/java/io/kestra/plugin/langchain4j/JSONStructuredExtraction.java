package io.kestra.plugin.langchain4j;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.ChatConfiguration;
import io.kestra.plugin.langchain4j.domain.ModelProvider;
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
@Schema(
    title = "Generate JSON-Structured Extraction with AI models.",
    description = "The task is currently compatible with OpenAI, Ollama, and Gemini models."
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "JSON Structured Extraction using Ollama",
            full = true,
            code = {
                """
                id: json_structured_extraction_ollama
                namespace: company.team
                task:
                    id: json_structured_extraction
                    type: io.kestra.core.plugin.langchain4j.JSONStructuredExtraction
                    jsonFields:
                      - name
                      - City
                    schemaName: Person
                    prompt: Hello, my name is John, I live in Paris
                    provider:
                      type: io.kestra.plugin.langchain4j.provider.Ollama
                      modelName: llama3
                      endpoint: http://localhost:11434
                """
            }
        ),
        @io.kestra.core.models.annotations.Example(
            title = "JSON Structured Extraction using OpenAI",
            full = true,
            code = {
                """
                id: json_structured_extraction_openai
                namespace: company.team
                task:
                    id: json_structured_extraction
                    type: io.kestra.core.plugin.langchain4j.JSONStructuredExtraction
                    jsonFields:
                      - name
                      - City
                    schemaName: Person
                    prompt: Hello, my name is John, I live in Paris
                    provider:
                      type: io.kestra.plugin.langchain4j.provider.OpenAI
                      apiKey: your_openai_api_key
                      modelName: GPT-4
                """
            }
        ),
        @io.kestra.core.models.annotations.Example(
            title = "JSON Structured Extraction using Anthropic",
            full = true,
            code = {
                """
                id: json_structured_extraction_anthropic
                namespace: company.team
                task:
                    id: json_structured_extraction
                    type: io.kestra.core.plugin.langchain4j.JSONStructuredExtraction
                    jsonFields:
                      - name
                      - City
                    schemaName: Person
                    prompt: Hello, my name is John, I live in Paris
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.Anthropic
                        apiKey: your_anthropic_api_key
                        modelName: claude-3-haiku-20240307
                """
            }
        ),
        @io.kestra.core.models.annotations.Example(
            title = "JSON Structured Extraction using DeepSeek",
            full = true,
            code = {
                """
                id: json_structured_extraction_deepseek
                namespace: company.team
                task:
                    id: json_structured_extraction
                    type: io.kestra.core.plugin.langchain4j.JSONStructuredExtraction
                    jsonFields:
                      - name
                      - City
                    schemaName: Person
                    prompt: Hello, my name is John, I live in Paris
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.DeepSeek
                        apiKey: your_deepseek_api_key
                        modelName: deepseek-chat
                """
            }
        ),
        @io.kestra.core.models.annotations.Example(
            title = "JSON Structured Extraction using Mistal AI",
            full = true,
            code = {
                """
                id: json_structured_extraction_openai
                namespace: company.team
                task:
                    id: json_structured_extraction
                    type: io.kestra.core.plugin.langchain4j.JSONStructuredExtraction
                    jsonFields:
                      - name
                      - City
                    schemaName: Person
                    prompt: Hello, my name is John, I live in Paris
                    provider:
                        type: io.kestra.plugin.langchain4j.provider.MistralAI
                        apiKey: your_mistral_api_key
                        modelName: mistral:7b
                """
            }
        )
    },
    beta = true
)
public class JSONStructuredExtraction extends Task implements RunnableTask<JSONStructuredExtraction.Output> {

    @Schema(title = "Text prompt", description = "The input prompt for the AI model.")
    @NotNull
    private Property<String> prompt;

    @Schema(title = "Schema Name", description = "The name of the JSON schema for structured extraction.")
    @NotNull
    private Property<String> schemaName;

    @Schema(title = "JSON Fields", description = "List of fields to extract from the text.")
    @NotNull
    private Property<List<String>> jsonFields;

    @Schema(title = "Language Model Provider")
    @NotNull
    @PluginProperty
    private ModelProvider provider;

    @Schema(title = "Chat configuration")
    @NotNull
    @PluginProperty
    @Builder.Default
    private ChatConfiguration configuration = ChatConfiguration.empty();

    @Override
    public JSONStructuredExtraction.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render input properties
        String renderedPrompt = runContext.render(prompt).as(String.class).orElseThrow();
        String renderedSchemaName = runContext.render(schemaName).as(String.class).orElseThrow();
        List<String> renderedJsonFields = Property.asList(jsonFields, runContext, String.class);

        // Get the appropriate model from the factory
        ChatModel model = this.provider.chatModel(runContext, configuration);

        // Build JSON schema
        ResponseFormat responseFormat = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(JsonSchema.builder()
                .name(renderedSchemaName)
                .rootElement(buildDynamicSchema(renderedJsonFields))
                .build())
            .build();

        // Build request
        ChatRequest chatRequest = ChatRequest.builder()
            .parameters(ChatRequestParameters.builder().responseFormat(responseFormat).build())
            .messages(UserMessage.from(renderedPrompt))
            .build();


        // Generate structured JSON output
        ChatResponse answer = model.chat(chatRequest);

        logger.debug("Generated Structured Extraction: {}", answer);

        return Output.builder()
            .schemaName(renderedSchemaName)
            .extractedJson(answer.aiMessage().text())
            .tokenUsage(answer.tokenUsage())
            .finishReason(answer.finishReason())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Schema Name", description = "The schema name used for the structured JSON extraction.")
        private String schemaName;

        @Schema(title = "Extracted JSON", description = "The structured JSON output.")
        private String extractedJson;

        @Schema(title = "Token usage")
        private TokenUsage tokenUsage;

        @Schema(title = "Finish reason")
        private FinishReason finishReason;
    }

    public static JsonObjectSchema buildDynamicSchema(List<String> fields) {
        JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
        fields.forEach(schemaBuilder::addStringProperty);
        schemaBuilder.required(fields);
        return schemaBuilder.build();
    }

}
