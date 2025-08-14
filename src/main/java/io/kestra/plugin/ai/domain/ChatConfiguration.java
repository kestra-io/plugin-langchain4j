package io.kestra.plugin.ai.domain;

import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatConfiguration {
    @Schema(title = "Temperature")
    private Property<Double> temperature;

    @Schema(title = "topK")
    private Property<Integer> topK;

    @Schema(title = "topP")
    private Property<Double> topP;

    @Schema(title = "seed")
    private Property<Integer> seed;

    @Schema(
        title = "Whether to log LLM requests",
        description = "Log will be send to the server log in DEBUG."
    )
    private Property<Boolean> logRequest;

    @Schema(
        title = "Whether to log LLM responses",
        description = "Log will be send to the server log in DEBUG."
    )
    private Property<Boolean> logResponses;

    public static ChatConfiguration empty() {
        return ChatConfiguration.builder().build();
    }
}
