package io.kestra.plugin.ai.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ToolExecution {
    private String requestId;
    private String requestName;
    private String requestArguments;
    private String result;

    public static ToolExecution from(dev.langchain4j.service.tool.ToolExecution toolExecution) {
        return ToolExecution.builder()
            .requestId(toolExecution.request().id())
            .requestName(toolExecution.request().name())
            .requestArguments(toolExecution.request().arguments())
            .result(toolExecution.result())
            .build();
    }
}
