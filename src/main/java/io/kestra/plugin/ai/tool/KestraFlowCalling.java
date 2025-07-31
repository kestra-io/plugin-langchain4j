package io.kestra.plugin.ai.tool;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.queues.QueueFactoryInterface;
import io.kestra.core.queues.QueueInterface;
import io.kestra.core.runners.DefaultRunContext;
import io.kestra.core.runners.FlowMetaStoreInterface;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.core.services.PluginDefaultService;
import io.kestra.plugin.ai.domain.ToolProvider;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.AllArgsConstructor; TODO needs to be added if we add some props
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
@SuperBuilder
@NoArgsConstructor
@Plugin(
    beta = true,
    examples =  {
        @Example(
            title = "Call a Kestra flow as a tool",
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
public class KestraFlowCalling extends ToolProvider {

    @Override
    public Map<ToolSpecification, ToolExecutor> tool(RunContext runContext) throws IllegalVariableEvaluationException {
        return Map.of(
            ToolSpecification.builder()
                .name("kestra_flow_calling")
                .description("This tool allow to execute a Kestra flow, it needs at least the namespace and identifier of the flow to execute.") // TODO I think this should be the description of the tool not of the flow, maybe a bit of both...
                .parameters(JsonObjectSchema.builder()
                    .addStringProperty("namespace", "The namespace of the flow to be executed.")
                    .addStringProperty("revision", "The revision of the flow to be executed.")
                    .addStringProperty("id", "The identifier of the flow to be executed.")
                    .required("namespace", "id")
                    .build())
                .build(),
            new KestraFlowToolExecutor((DefaultRunContext) runContext)
        );
    }

    static class KestraFlowToolExecutor implements ToolExecutor {
        private final DefaultRunContext runContext;

        KestraFlowToolExecutor(DefaultRunContext runContext) {
            this.runContext = runContext;
        }

        @Override
        @SuppressWarnings("unchecked")
        public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
            runContext.logger().debug("Tool execution request: {}", toolExecutionRequest);
            try {
                var targetFlow = JacksonMapper.toMap(toolExecutionRequest.arguments());
                var flowMetaStoreInterface = runContext.getApplicationContext().getBean(FlowMetaStoreInterface.class);
                var pluginDefaultService = runContext.getApplicationContext().getBean(PluginDefaultService.class);
                var flowInfo = runContext.flowInfo();

                var rNamespace = (String) Objects.requireNonNull(targetFlow.get("namespace"));
                var rFlowId = (String) Objects.requireNonNull(targetFlow.get("id"));
                var rRevision = Optional.ofNullable((Integer) targetFlow.get("revision"));

                // TODO add description to the flow interface + update Execution.newExecution(flow, labels) to take a FlowInterface
                var flowInterface = flowMetaStoreInterface.findByIdFromTask(flowInfo.tenantId(), rNamespace, rFlowId, rRevision, flowInfo.tenantId(), flowInfo.namespace(), flowInfo.id()).orElseThrow();
                var flow = pluginDefaultService.injectAllDefaults(flowInterface, runContext.logger());
                // create a new execution TODO labels/inputs/...
                var execution = Execution.newExecution(flow, Collections.emptyList());

                var executionQueue = (QueueInterface<Execution>) runContext.getApplicationContext().getBean(QueueInterface.class, Qualifiers.byName(QueueFactoryInterface.EXECUTION_NAMED));
                executionQueue.emit(execution);

                return "Success";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
