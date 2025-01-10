package io.kestra.plugin.dto;

import io.kestra.plugin.enums.EChatType;
import lombok.Builder;

@Builder
public record ChatMessageDTO(EChatType type, String content) {}
