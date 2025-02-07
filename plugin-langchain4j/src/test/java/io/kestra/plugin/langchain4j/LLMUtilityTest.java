package io.kestra.plugin.langchain4j;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import io.kestra.plugin.langchain4j.dto.ChatMessage;
import io.kestra.plugin.langchain4j.enums.ChatType;
import io.kestra.plugin.langchain4j.utils.LLMUtility;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LLMUtilityTest {

    @Test
    void testConvertFromDTOs() {
        // GIVEN: A list of ChatMessageDTOs
        List<ChatMessage> dtos = List.of(
            new ChatMessage(ChatType.USER, "Hello, my name is John"),
            new ChatMessage(ChatType.AI, "Hello John, nice to meet you")
        );

        // WHEN: Converting from DTOs to ChatMessage
        List<dev.langchain4j.data.message.ChatMessage> chatMessages = LLMUtility.convertFromDTOs(dtos);

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
        List<ChatMessage> dtos = List.of(
            new ChatMessage(null, "Invalid type")
        );

        // WHEN & THEN: Converting from DTOs should throw an exception
        assertThrows(IllegalArgumentException.class, () -> LLMUtility.convertFromDTOs(dtos));
    }
}
