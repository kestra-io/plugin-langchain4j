package io.kestra.plugin.langchain4j.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

public class ChatModelFactory {

    public static ChatLanguageModel createModel(Provider provider, String apiKey, String modelName, String ollamaEndpoint) {
        return switch (provider) {
            case GOOGLE_GEMINI -> GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
            case OPENAI -> OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
            case OLLAMA -> OllamaChatModel.builder()
                .baseUrl(ollamaEndpoint)
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true)
                .build();
        };
    }
}
