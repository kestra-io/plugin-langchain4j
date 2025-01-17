package io.kestra.plugin.langchain4j.dto;

import io.kestra.plugin.langchain4j.enums.ChatType;
import lombok.Builder;

@Builder
public record ChatMessageDTO(ChatType type, String content) {}
