package io.kestra.plugin.ai.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolExecutor;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.ai.domain.*;
import io.kestra.plugin.ai.rag.ChatCompletion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.*;

import static io.kestra.core.utils.Rethrow.throwConsumer;
import static io.kestra.core.utils.Rethrow.throwFunction;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractAiAgentWorkflow extends Task implements RunnableTask<ChatCompletion.Output> {
    @Schema(title = "Language Model Provider")
    @NotNull
    @PluginProperty
    private ModelProvider provider;

    @Schema(title = "Chat configuration")
    @NotNull
    @PluginProperty
    @Builder.Default
    private ChatConfiguration chatConfiguration = ChatConfiguration.empty();

    @Schema(title = "The list of agents")
    @NotNull
    @NotEmpty
    @Valid
    private List<Agent> agents;

    private Property<Map<String, Object>> inputs;

    // TODO allow A2A agents as subagents

    @Override
    public ChatCompletion.Output run(RunContext runContext) throws Exception {
        List<ToolProvider> allToolProviders = new ArrayList<>();

        try {
            Object[] assistants = agents.stream()
                .map(throwFunction(agent -> {
                    List<ToolProvider> toolProviders = runContext.render(agent.tools).asList(ToolProvider.class);
                    allToolProviders.addAll(toolProviders);
                    return AgenticServices.agentBuilder(SubAgentInterface.class)
                        .chatModel(provider.chatModel(runContext, chatConfiguration))
                        .tools(buildTools(runContext, toolProviders))
                        .outputName(runContext.render(agent.outputName).as(String.class).orElseThrow())
                        .context(throwFunction(agenticScope -> {
                            agenticScope.writeState("input", runContext.render(agent.prompt).as(String.class).orElseThrow());
                            return null;
                        }))
                        .build();
                }))
                .toArray();

            Map<String, Object> rInputs = runContext.render(inputs).asMap(String.class, Object.class);

            WorkflowAgent workflowAgent = workflowAgent(runContext, assistants);
            Response<AiMessage> completion = workflowAgent.invoke(rInputs);
            runContext.logger().debug("Generated Completion: {}", completion.content());

            return ChatCompletion.Output.builder()
                .completion(completion.content().text())
                .tokenUsage(TokenUsage.from(completion.tokenUsage()))
                .finishReason(completion.finishReason())
                .build();
        } finally {
            allToolProviders.forEach(tool -> tool.close(runContext));
        }
    }

    protected abstract WorkflowAgent workflowAgent(RunContext runContext, Object[] assistants) throws IllegalVariableEvaluationException;

    private Map<ToolSpecification, ToolExecutor> buildTools(RunContext runContext, List<ToolProvider> toolProviders) throws IllegalVariableEvaluationException {
        if (toolProviders.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        toolProviders.forEach(throwConsumer(provider -> tools.putAll(provider.tool(runContext))));
        return tools;
    }


    protected interface WorkflowAgent {
        @dev.langchain4j.agentic.Agent
        Response<AiMessage> invoke(Map<String, Object> inputs);
    }

    protected interface SubAgentInterface {
        @UserMessage("""
            You are an agent, answer to the following query.
            {{input}}"""
        )
        @dev.langchain4j.agentic.Agent
        Response<AiMessage> invoke();
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
    }

    @Getter
    @Builder
    public static class Agent {
        @Schema(title = "Text prompt", description = "The input prompt for the language model")
        @NotNull
        protected Property<String> prompt;

        @Schema(title = "Output name", description = "The name of the output, you can use it in the prompt of other agents via {% raw %}{{outputName}}{% endraw %}")
        @NotNull
        protected Property<String> outputName;

        @Schema(title = "Tools that the LLM may use to augment its response")
        private Property<List<ToolProvider>> tools;

        // TODO allow providing a different model for each agent

        // TODO allow setting a memory

        // TODO allow system message when available
    }
}
