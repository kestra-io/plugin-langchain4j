package io.kestra.plugin.langchain4j.dto;

import io.kestra.plugin.langchain4j.enums.ChatType;
import lombok.Builder;

@Builder
public record ChatMessage(ChatType type, String content) {}
