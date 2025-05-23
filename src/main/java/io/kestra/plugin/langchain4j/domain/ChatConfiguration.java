package io.kestra.plugin.langchain4j.domain;

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

    public static ChatConfiguration empty() {
        return ChatConfiguration.builder().build();
    }
}
