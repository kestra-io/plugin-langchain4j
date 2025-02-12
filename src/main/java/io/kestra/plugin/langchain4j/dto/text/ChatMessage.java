package io.kestra.plugin.langchain4j.dto.text;

import lombok.Builder;

@Builder
public record ChatMessage(ChatType type, String content) {}
