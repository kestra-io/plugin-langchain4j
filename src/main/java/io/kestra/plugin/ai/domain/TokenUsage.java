package io.kestra.plugin.ai.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TokenUsage {
    private Integer inputTokenCount;
    private Integer outputTokenCount;
    private Integer totalTokenCount;

    public static TokenUsage from(dev.langchain4j.model.output.TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return null;
        }

        return TokenUsage.builder()
            .inputTokenCount(tokenUsage.inputTokenCount())
            .outputTokenCount(tokenUsage.outputTokenCount())
            .totalTokenCount(tokenUsage.totalTokenCount())
            .build();
    }
}
