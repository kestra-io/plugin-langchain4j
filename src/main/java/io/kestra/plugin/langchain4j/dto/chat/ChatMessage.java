package io.kestra.plugin.langchain4j.dto.chat;

import lombok.Builder;

@Builder
public record ChatMessage(ChatType type, String content) {}
