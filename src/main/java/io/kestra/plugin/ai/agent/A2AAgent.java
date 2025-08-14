package io.kestra.plugin.ai.agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.Result;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.ListUtils;
import io.kestra.plugin.ai.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Call a remote AI agent via the A2A protocol.")
@Plugin(
    examples = {
        @Example(
            full = true,
            title = """
                TODO""",
            code = """
                TODO"""
        ),
    },
    beta = true
)
public class A2AAgent extends Task implements RunnableTask<A2AAgent.Output> {
    @Schema(title = "Server URL", description = "The URL of the remote agent A2A server")
    @NotNull
    protected Property<String> serverUrl;

    @Schema(title = "Text prompt", description = "The input prompt for the language model")
    @NotNull
    protected Property<String> prompt;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String rServerUrl = runContext.render(serverUrl).as(String.class).orElseThrow();
        runContext.logger().info("Calling a remote agent via the A2A protocol on URL {}", rServerUrl);
        Agent agent = AgenticServices.a2aBuilder(rServerUrl, Agent.class)
            .build();

        String renderedPrompt = runContext.render(prompt).as(String.class).orElseThrow();
        Result<AiMessage> completion = agent.invoke(renderedPrompt);
        runContext.logger().debug("Generated Completion: {}", completion.content());

        return Output.builder()
            .completion(completion.content().text())
            .tokenUsage(TokenUsage.from(completion.tokenUsage()))
            .finishReason(completion.finishReason())
            .toolExecutions(ListUtils.emptyOnNull(completion.toolExecutions()).stream().map(ToolExecution::from).toList())
            .build();
    }

    interface Agent {
        Result<AiMessage> invoke(String userMessage);
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Generated text completion", description = "The result of the text completion")
        private String completion;

        @Schema(title = "Token usage")
        private TokenUsage tokenUsage;

        @Schema(title = "Finish reason")
        private FinishReason finishReason;

        @Schema(title = "Tool executions")
        private List<ToolExecution> toolExecutions;
    }
}