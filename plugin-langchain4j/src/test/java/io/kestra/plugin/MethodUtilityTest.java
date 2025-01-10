package io.kestra.plugin;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import io.kestra.plugin.dto.ChatMessageDTO;
import io.kestra.plugin.enums.EChatType;
import io.kestra.plugin.utils.MethodUtility;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MethodUtilityTest {

    @Test
    void testConvertFromDTOs() {
        // GIVEN: A list of ChatMessageDTOs
        List<ChatMessageDTO> dtos = List.of(
            new ChatMessageDTO(EChatType.USER, "Hello, my name is John"),
            new ChatMessageDTO(EChatType.AI, "Hello John, nice to meet you")
        );

        // WHEN: Converting from DTOs to ChatMessage
        List<ChatMessage> chatMessages = MethodUtility.convertFromDTOs(dtos);

        // THEN: Validate the conversion
        assertThat(chatMessages, hasSize(2));
        assertThat(chatMessages.get(0), instanceOf(UserMessage.class));
        assertThat(chatMessages.get(1), instanceOf(AiMessage.class));
        assertThat(((UserMessage) chatMessages.get(0)).singleText(), is("Hello, my name is John"));
        assertThat(((AiMessage) chatMessages.get(1)).text(), is("Hello John, nice to meet you"));
    }

    @Test
    void testConvertFromDTOsInvalidType() {
        // GIVEN: A list with an unsupported type
        List<ChatMessageDTO> dtos = List.of(
            new ChatMessageDTO(null, "Invalid type")
        );

        // WHEN & THEN: Converting from DTOs should throw an exception
        assertThrows(IllegalArgumentException.class, () -> MethodUtility.convertFromDTOs(dtos));
    }
}
