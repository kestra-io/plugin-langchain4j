package io.kestra.plugin.enums;

import lombok.Getter;

@Getter
public enum ChatType {
    AI("AI"), USER("User");
    private String value;

    ChatType(String ai) {
    }
}
