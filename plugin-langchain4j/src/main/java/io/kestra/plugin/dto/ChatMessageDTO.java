package io.kestra.plugin.dto;

import io.kestra.plugin.enums.ChatType;
import lombok.Builder;

@Builder
public record ChatMessageDTO(ChatType type, String content) {}
