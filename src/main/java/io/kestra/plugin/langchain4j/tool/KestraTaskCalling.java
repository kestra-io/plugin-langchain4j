package io.kestra.plugin.langchain4j.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.service.tool.ToolExecutor;
import io.kestra.core.docs.JsonSchemaGenerator;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.plugins.PluginRegistry;
import io.kestra.core.runners.DefaultRunContext;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.langchain4j.domain.ToolProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.lang.reflect.InvocationTargetException;
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
            title = "TODO",
            full = true,
            code = {
                """
                TODO
                """
            }
        ),
    }
)
@JsonDeserialize
@Schema(
    title = "TODO"
)
public class KestraTaskCalling extends ToolProvider {
    @Schema(title = "Task type")
    @NotNull
    private Property<String> task;

    @Override
    public Map<ToolSpecification, ToolExecutor> tool(RunContext runContext) throws IllegalVariableEvaluationException {
        var defaultRunContext = (DefaultRunContext) runContext;
        var pluginRegistry = defaultRunContext.getApplicationContext().getBean(PluginRegistry.class);
        var jsonSchemaGenerator = defaultRunContext.getApplicationContext().getBean(JsonSchemaGenerator.class);
        var rTask = runContext.render(task).as(String.class).orElseThrow();
        var clazz = pluginRegistry.findClassByIdentifier(rTask);
        if (!RunnableTask.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("The KestraTaskCalling can only execute runnable tasks");
        }
        var description = clazz.getAnnotation(Schema.class).title();

        var schema = jsonSchemaGenerator.properties(Task.class, (Class<Task>) clazz);
        var parameters = parametersFrom(schema);

        return Map.of(ToolSpecification.builder()
            .name("kestra_task_" + rTask)
            .description(description)
            .parameters(parameters)
            .build(), new KestraTaskToolExecutor((Class<RunnableTask<?>>) clazz, runContext));
    }

    private JsonObjectSchema parametersFrom(Map<String, Object> schema) {

        return JsonObjectSchema.builder()
            .addProperties(mapProperties((Map<String, Object>) schema.get("properties")))
            .required((List<String>) schema.get("required"))
            .definitions(mapDefinitions((Map<String, Object>) schema.get("$defs")))
            .build();
    }

    private Map<String, JsonSchemaElement> mapDefinitions(Map<String, Object> defs) {
        // FIXME
        return null;
    }

    private Map<String, JsonSchemaElement>  mapProperties(Map<String, Object> properties) {
        return properties.entrySet().stream()
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

    private JsonSchemaElement mapSchema(Map<String, Object> schema) {
        var type = (String) schema.get("type");
        var _enum =  (List<String>) schema.get("enum");
        var title =  (String) schema.get("title");
        var anyOf = (List<Map<String, Object>>) schema.get("anyOf");
        if (_enum != null) {
            return JsonEnumSchema.builder().description(title).enumValues(_enum).build();
        }
        if (anyOf != null) {
            // TODO anyOf seems to be not supported everywhere so we coalesce to a String
//            var anyOfSchemas = anyOf.stream().map(s -> mapSchema(s)).toList();
//            return JsonAnyOfSchema.builder().description(title).anyOf(anyOfSchemas).build();
            return JsonStringSchema.builder().description(title).build();
        }
        if (type == null) { // we should not arrives here but this is a safeguard
            return new JsonNullSchema();
        }
        // TODO array and object, null, anyOff
        return switch (type) {
            case "number" -> JsonNumberSchema.builder().description(title).build();
            case "integer" -> JsonIntegerSchema.builder().description(title).build();
            case "boolean" -> JsonBooleanSchema.builder().description(title).build();
            // we coalesce other types to String for now...
            default -> JsonStringSchema.builder().description(title).build();
        };
    }

    static class KestraTaskToolExecutor implements ToolExecutor {
        private final Class<RunnableTask<?>> task;
        private final RunContext runContext;

        KestraTaskToolExecutor(Class<RunnableTask<?>> task,  RunContext runContext) {
            this.task = task;
            this.runContext = runContext;
        }

        @Override
        public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
            System.out.println("Task Calling! :" + toolExecutionRequest);
            try {
                RunnableTask<?> runnable = JacksonMapper.ofJson().readValue(toolExecutionRequest.arguments(), task);
                runnable.run(runContext);
                return "Success";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
