package io.kestra.plugin.ai.tool;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.service.tool.ToolExecutor;
import io.kestra.core.docs.JsonSchemaGenerator;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.Output;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.DefaultRunContext;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.core.utils.MapUtils;
import io.kestra.plugin.ai.domain.ToolProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Plugin(
    beta = true,
    examples =  {
        @Example(
            title = "Call a Kestra runnable task as a tool",
            full = true,
            code = {
                """
                id: kestra-tool
                    namespace: company.team

                    tasks:
                      - id: first
                        type: io.kestra.plugin.ai.completion.ChatCompletion
                        provider:
                          type: io.kestra.plugin.ai.provider.GoogleGemini
                          modelName: gemini-2.5-flash
                          apiKey: "{{ secret('GOOGLE_API_KEY') }}"
                        tools:
                          - type: io.kestra.plugin.ai.tool.KestraTaskCalling
                            tasks:
                              - id: log
                                type: io.kestra.plugin.core.log.Log
                                message: "{{agent.message}}"
                        messages:
                          - type: SYSTEM
                            content: You are an AI agent, please use the provided tool to fulfill the request.
                          - type: USER
                            content: "I want to log the following message: 'Hello World!'"
                """
            }
        ),
    }
)
@JsonDeserialize
@Schema(
    title = "Call a Kestra runnable task as a tool"
)
public class KestraTaskCalling extends ToolProvider {
    @Schema(title = "List of Kestra runnable tasks")
    @NotNull
    private List<Task> tasks;

    @Override
    public Map<ToolSpecification, ToolExecutor> tool(RunContext runContext) throws IllegalVariableEvaluationException {
        var defaultRunContext = (DefaultRunContext) runContext;
        var jsonSchemaGenerator = defaultRunContext.getApplicationContext().getBean(JsonSchemaGenerator.class);

        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        for (Task task : tasks) {
            // First, check that the task is a runnable task.
            if (!(task instanceof RunnableTask)) {
                throw new IllegalArgumentException("KestraTaskCalling is only capable of calling runnable tasks but '" + task.getId() + "' is not a runnable task.");
            }

            var description = task.getClass().getAnnotation(Schema.class).title();
            var schema = jsonSchemaGenerator.properties(Task.class, task.getClass());
            var parameters = parametersFrom(schema, description); // TODO we may need to restrict to what's intended to be asked to the AI
            var toolSpecification = ToolSpecification.builder()
                .name("kestra_task_" + task.getId())
                .description("This tool allows to call a Kestra task. A Kestra task will respond with its output that is a map of key value from where you can extract variables.")
                .parameters(parameters)
                .build();
            runContext.logger().debug("Tool specification: {}", toolSpecification);
            var toolExecutor = new KestraTaskToolExecutor((RunnableTask<?>) task, runContext);
            tools.put(toolSpecification, toolExecutor);
        }

        return tools;
    }

    @SuppressWarnings("unchecked")
    private JsonObjectSchema parametersFrom(Map<String, Object> schema, String description) {
        var definitions = mapDefinitions((Map<String, Object>) schema.get("$defs"));
        var properties = mapProperties((Map<String, Object>) schema.get("properties"));

        // some LLM didn't support definitions, so we will remove them and replace them by their schema.
        definitions = replaceDefinitions(definitions, definitions);
        properties = replaceDefinitions(properties, definitions);

        return JsonObjectSchema.builder()
            .addProperties(properties)
            .required((List<String>) schema.get("required"))
            .definitions(definitions)
            .description(description)
            .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, JsonSchemaElement> mapDefinitions(Map<String, Object> defs) {
        return MapUtils.emptyOnNull(defs).entrySet().stream()
            .map(entry -> {
                var schema = (Map<String, Object>) entry.getValue();
                var jsonSchemaElement = parametersFrom(schema, null);
                return Map.entry(
                    entry.getKey(),
                    jsonSchemaElement
                );
            })
            .collect(Collectors.toMap(
                entry -> entry.getKey(),
                entry -> entry.getValue()
            ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, JsonSchemaElement>  mapProperties(Map<String, Object> properties) {
        return MapUtils.emptyOnNull(properties).entrySet().stream()
            .filter(prop -> !prop.getKey().equals("type"))
            .map(entry -> {
                var schema = (Map<String, Object>) entry.getValue();
                var jsonSchemaElement = mapSchema(schema);
                return Map.entry(
                    entry.getKey(),
                    jsonSchemaElement
                );
            })
            .filter(entry -> !(entry.getValue() instanceof JsonNullSchema))
            .collect(Collectors.toMap(
                entry -> entry.getKey(),
                entry -> entry.getValue()
            ));
    }

    @SuppressWarnings("unchecked")
    private JsonSchemaElement mapSchema(Map<String, Object> schema) {
        var type = (String) schema.get("type");
        var title =  (String) schema.get("title");
        var _enum =  (List<String>) schema.get("enum");
        var anyOf = (List<Map<String, Object>>) schema.get("anyOf");
        var ref = (String) schema.get("$ref");
        if (_enum != null) {
            return JsonEnumSchema.builder().description(title).enumValues(_enum).build();
        }
        if (anyOf != null) {
            // anyOf is not supported by all LLM (for ex gemini).
            // So instead, we try to find a type which is not a string and return it as it's supposed to be more specific.
            return anyOf.stream()
                .filter(subSchema -> !"string".equals(subSchema.get("type")))
                .map(subSchema -> mapSchema(subSchema))
                .findAny()
                .orElse(JsonStringSchema.builder().description(title).build());
        }
        if (ref != null) {
            // reference starts with "#/$defs/"
            String referenceType = ref.substring(8);
            return JsonReferenceSchema.builder().reference(referenceType).build();
        }

        return switch (type) {
            case "number" -> JsonNumberSchema.builder().description(title).build();
            case "integer" -> JsonIntegerSchema.builder().description(title).build();
            case "boolean" -> JsonBooleanSchema.builder().description(title).build();
            case "null" -> new JsonNullSchema();
            case "object" -> JsonObjectSchema.builder().description(title).build();
            case "array" -> JsonArraySchema.builder().description(title).items(mapSchema((Map<String, Object>) schema.get("items"))).build();
            case null -> new JsonNullSchema();
            // we coalesce other types to String for now...
            default -> JsonStringSchema.builder().description(title).build();
        };
    }

    private Map<String, JsonSchemaElement> replaceDefinitions(Map<String, JsonSchemaElement> properties, Map<String, JsonSchemaElement> definitions) {
        return properties.entrySet().stream()
            .map(entry -> {
                JsonSchemaElement schema = switch(entry.getValue()) {
                    case JsonReferenceSchema jsonReferenceSchema -> definitions.getOrDefault(jsonReferenceSchema.reference(),
                        JsonObjectSchema.builder().description(jsonReferenceSchema.description()).build());
                    case JsonObjectSchema jsonObjectSchema -> JsonObjectSchema.builder()
                        .description(jsonObjectSchema.description())
                        .required(jsonObjectSchema.required()).
                        addProperties(replaceDefinitions(jsonObjectSchema.properties(), definitions))
                        .build();
                    default -> entry.getValue();
                };
                return Map.entry(entry.getKey(), schema);
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static class KestraTaskToolExecutor implements ToolExecutor {
        private final RunnableTask<?> task;
        private final RunContext runContext;

        KestraTaskToolExecutor(RunnableTask<?> task,  RunContext runContext) {
            this.task = task;
            this.runContext = runContext;
        }

        @Override
        public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
            runContext.logger().debug("Tool execution request: {}", toolExecutionRequest);
            try {
                // merge task properties with tool execution arguments
                Map<String, Object> taskProperties = JacksonMapper.toMap(task);
                Map<String, Object> arguments = JacksonMapper.toMap(toolExecutionRequest.arguments());
                RunnableTask<?> runnable = JacksonMapper.ofJson().convertValue(MapUtils.merge(taskProperties, arguments), task.getClass());
                Output output = runnable.run(runContext);
                if (output!= null) {
                    Map<String, Object> outputMap = output.toMap();
                    if (!MapUtils.isEmpty(outputMap)) {
                        return JacksonMapper.ofJson().writeValueAsString(outputMap);
                    }
                }
                // we return Success here so when a task has no output but reply successfully, the LLM knows it and didn't re-call the task
                return "Success";
            } catch (Exception e) {
                // TODO we may instead send the error to the LLM so it can decide to retry it or not
                throw new RuntimeException(e);
            }
        }
    }
}
