package io.kestra.plugin.ai.domain;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.service.Result;
import io.kestra.core.utils.ListUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
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

    @Schema(title = "Intermediate responses")
    private List<AIResponse> intermediateResponses;

    public static AIOutput from(Result<AiMessage> result) {
        return AIOutput.builder()
            .completion(result.content().text())
            .tokenUsage(TokenUsage.from(result.tokenUsage()))
            .finishReason(result.finishReason())
            .toolExecutions(ListUtils.emptyOnNull(result.toolExecutions()).stream().map(ToolExecution::from).toList())
            .intermediateResponses(ListUtils.emptyOnNull(result.intermediateResponses().stream()
                .map(resp -> AIResponse.from(resp))
                .toList())
            )
            .build();
    }

    @Getter
    @Builder
    public static class AIResponse {
        @Schema(title = "Response identifier")
        private String id;

        @Schema(title = "Generated text completion", description = "The result of the text completion")
        private String completion;

        @Schema(title = "Token usage")
        private TokenUsage tokenUsage;

        @Schema(title = "Finish reason")
        private FinishReason finishReason;

        @Schema(title = "Tool execution requests")
        private List<ToolExecutionRequest>  toolExecutionRequests;

        static AIResponse from(ChatResponse chatResponse) {
            return AIResponse.builder()
                .id(chatResponse.id())
                .completion(chatResponse.aiMessage().text())
                .tokenUsage(TokenUsage.from(chatResponse.tokenUsage()))
                .finishReason(chatResponse.finishReason())
                .toolExecutionRequests(ListUtils.emptyOnNull(chatResponse.aiMessage().toolExecutionRequests()).stream()
                    .map(req -> ToolExecutionRequest.from(req))
                    .toList())
                .build();
        }

        @Getter
        @Builder
        public static class ToolExecutionRequest {
            @Schema(title = "Tool execution request identifier")
            private String id;

            @Schema(title = "Tool name")
            private String name;

            @Schema(title = "Tool request arguments")
            private String arguments;

            static ToolExecutionRequest from(dev.langchain4j.agent.tool.ToolExecutionRequest toolExecutionRequest) {
                return ToolExecutionRequest.builder()
                    .id(toolExecutionRequest.id())
                    .name(toolExecutionRequest.name())
                    .arguments(toolExecutionRequest.arguments())
                    .build();

            }
        }
    }
}
