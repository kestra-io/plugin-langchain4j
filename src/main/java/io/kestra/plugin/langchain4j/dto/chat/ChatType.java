package io.kestra.plugin.langchain4j.dto.chat;

import lombok.Getter;

@Getter
public enum ChatType {
    AI("AI"), USER("User");
    private String value;

    ChatType(String ai) {
    }
}
