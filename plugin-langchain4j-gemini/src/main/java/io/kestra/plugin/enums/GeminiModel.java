package io.kestra.plugin.enums;

import lombok.Getter;

@Getter
public enum GeminiModel {
    GEMINI_1_5_FLASH("gemini-1.5-flash"),
    GEMINI_1_5_FLASH_EXP("gemini-2.0-flash-exp"),
    GEMINI_1_5_PRO("gemini-1.5-pro"),
    GEMINI_1_0_PRO("gemini-1.0-pro"),
    AQA("aqa");
    private final String name;

    GeminiModel(String name) {
        this.name = name;
    }

}
