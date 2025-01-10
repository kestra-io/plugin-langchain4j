package io.kestra.plugin.enums;

import lombok.Getter;

@Getter
public enum EChatType {
    AI("Ai"), USER("User");
    private String value;

    EChatType(String ai) {
    }
}
