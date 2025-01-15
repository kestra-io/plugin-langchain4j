package io.kestra.plugin.utils;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import io.kestra.plugin.dto.ChatMessageDTO;
import io.kestra.plugin.enums.ChatType;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class LLMUtility {

    public static List<ChatMessage> convertFromDTOs(List<ChatMessageDTO> dtos) {
        return dtos.stream()
            .map(dto -> {
                if (ChatType.USER.equals(dto.type())) {
                    return UserMessage.userMessage(dto.content());
                } else if (ChatType.AI.equals(dto.type())) {
                    return AiMessage.aiMessage(dto.content());
                }
                throw new IllegalArgumentException("Unsupported Chat Message type");
            })
            .collect(Collectors.toList());
    }
}
