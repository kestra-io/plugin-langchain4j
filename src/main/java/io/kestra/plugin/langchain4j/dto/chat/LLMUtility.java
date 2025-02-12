package io.kestra.plugin.langchain4j.dto.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class LLMUtility {

    public static List<dev.langchain4j.data.message.ChatMessage> convertFromDTOs(List<ChatMessage> dtos) {
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
