package io.kestra.plugin.langchain4j.dto.image;

public enum Size {
    SMALL("256x256"),
    MEDIUM("512x512"),
    LARGE("1024x1024");

    private final String value;

    Size(String value) {
        this.value = value;
    }

    public String getSize() {
        return value;
    }

}