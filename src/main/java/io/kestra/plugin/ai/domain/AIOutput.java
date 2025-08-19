package io.kestra.plugin.ai.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.service.Result;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.ListUtils;
import io.kestra.plugin.ai.AIUtils;
import io.kestra.plugin.ai.provider.TimingChatModelListener;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.time.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.kestra.core.utils.Rethrow.throwFunction;

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

    @Schema(title = "Request duration in milliseconds")
    private Long requestDuration;

    public static AIOutput from(RunContext runContext, Result<AiMessage> result) throws JsonProcessingException {
        return AIOutput.builder()
            .completion(result.content().text())
            .tokenUsage(TokenUsage.from(result.tokenUsage()))
            .finishReason(result.finishReason())
            .toolExecutions(ListUtils.emptyOnNull(result.toolExecutions()).stream()
                .map(throwFunction(toolExecution -> ToolExecution.from(toolExecution)))
                .toList()
            )
            .intermediateResponses(ListUtils.emptyOnNull(result.intermediateResponses().stream()
                .map(throwFunction(resp -> AIResponse.from(runContext, resp)))
                .toList())
            )
            .requestDuration(extractTiming(runContext, result.finalResponse().id()))
            .build();
    }

    private static Long extractTiming(RunContext runContext, String id) {
        if (id != null) {
            StopWatch timer = TimingChatModelListener.getTimer(id);
            return timer.getTime(TimeUnit.MILLISECONDS);
        } else {
            runContext.logger().info("The model provider didn't include any identifier in its response, timing the response is not possible");
            return null;
        }
    }

    @Builder
    @Getter
    public static class ToolExecution {
        private String requestId;
        private String requestName;
        private Map<String, Object> requestArguments;
        private String result;

        public static ToolExecution from(dev.langchain4j.service.tool.ToolExecution toolExecution) throws JsonProcessingException {
            return ToolExecution.builder()
                .requestId(toolExecution.request().id())
                .requestName(toolExecution.request().name())
                .requestArguments(AIUtils.parseJson(toolExecution.request().arguments()))
                .result(toolExecution.result())
                .build();
        }
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

        @Schema(title = "Request duration in milliseconds")
        private Long requestDuration;

        static AIResponse from(RunContext runContext, ChatResponse chatResponse) throws JsonProcessingException {
            return AIResponse.builder()
                .id(chatResponse.id())
                .completion(chatResponse.aiMessage().text())
                .tokenUsage(TokenUsage.from(chatResponse.tokenUsage()))
                .finishReason(chatResponse.finishReason())
                .toolExecutionRequests(ListUtils.emptyOnNull(chatResponse.aiMessage().toolExecutionRequests()).stream()
                    .map(throwFunction(req -> ToolExecutionRequest.from(req)))
                    .toList())
                .requestDuration(extractTiming(runContext, chatResponse.id()))
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
            private Map<String, Object> arguments;

            static ToolExecutionRequest from(dev.langchain4j.agent.tool.ToolExecutionRequest toolExecutionRequest) throws JsonProcessingException {
                return ToolExecutionRequest.builder()
                    .id(toolExecutionRequest.id())
                    .name(toolExecutionRequest.name())
                    .arguments(AIUtils.parseJson(toolExecutionRequest.arguments()))
                    .build();

            }
        }
    }
}
