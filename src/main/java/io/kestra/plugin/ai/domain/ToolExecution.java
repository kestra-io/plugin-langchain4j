package io.kestra.plugin.ai.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.kestra.plugin.ai.AIUtils;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
public class ToolExecution {
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
