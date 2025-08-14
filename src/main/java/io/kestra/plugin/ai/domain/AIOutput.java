package io.kestra.plugin.ai.domain;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.service.Result;
import io.kestra.core.utils.ListUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
public class AIOutput implements io.kestra.core.models.tasks.Output {
    @Schema(title = "Generated text completion", description = "The result of the text completion")
    private String completion;

    @Schema(title = "Token usage")
    private TokenUsage tokenUsage;

    @Schema(title = "Finish reason")
    private FinishReason finishReason;

    @Schema(title = "Tool executions")
    private List<ToolExecution> toolExecutions;

    public static AIOutput from(Result<AiMessage> result) {
        return AIOutput.builder()
            .completion(result.content().text())
            .tokenUsage(TokenUsage.from(result.tokenUsage()))
            .finishReason(result.finishReason())
            .toolExecutions(ListUtils.emptyOnNull(result.toolExecutions()).stream().map(ToolExecution::from).toList())
            .build();
    }
}
