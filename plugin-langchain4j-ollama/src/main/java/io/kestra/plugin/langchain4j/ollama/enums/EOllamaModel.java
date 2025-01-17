package io.kestra.plugin.langchain4j.ollama.enums;

import lombok.Getter;

@Getter
public enum EOllamaModel {
    OLLAMA3("llama3"),
    OLLAMA3_3("llama3.3"),
    OLLAMA3_2("llama3.2"),
    OLLAMA3_1("llama3.1"),
    PHI4("phi4"),
    QWQ("qwq"),
    TINY_DOLPHIN("tinydolphin"),
    NOMIC_EMBED_TEXT("nomic-embed-text"),
    GEMMA("gemma");

    private final String name;

    EOllamaModel(String name) {
        this.name = name;
    }
}
