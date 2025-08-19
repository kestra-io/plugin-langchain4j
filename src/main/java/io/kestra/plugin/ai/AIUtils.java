package io.kestra.plugin.ai;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.ai.domain.TokenUsage;
import io.kestra.plugin.ai.domain.ToolProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.kestra.core.utils.Rethrow.throwConsumer;

public final class AIUtils {

    private AIUtils() {
        // utility class pattern
    }

    public static Map<ToolSpecification, ToolExecutor> buildTools(RunContext runContext, List<ToolProvider> toolProviders) throws IllegalVariableEvaluationException {
        if (toolProviders.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        toolProviders.forEach(throwConsumer(provider -> tools.putAll(provider.tool(runContext))));
        return tools;
    }

    public static void sendMetrics(RunContext runContext, TokenUsage tokenUsage) {
        runContext.metric(Counter.of("ai.token.usage.input.count", "Large Language Model (LLM) input token count", tokenUsage.getInputTokenCount()));
        runContext.metric(Counter.of("ai.token.usage.output.count", "Large Language Model (LLM) output token count", tokenUsage.getOutputTokenCount()));
        runContext.metric(Counter.of("ai.token.usage.token.count", "Large Language Model (LLM) total token count", tokenUsage.getTotalTokenCount()));
    }
}
