package io.kestra.plugin.langchain4j.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.kestra.core.models.property.Property;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(title = "Provider Configuration", description = "Configuration settings for different providers")
public class ProviderConfig {

    @Schema(title = "Provider Type", description = "Choose between GOOGLE_GEMINI, OPEN_AI, or OLLAMA")
    @NotNull
    private Provider type;

    @Schema(title = "API Key", description = "API key for the provider (if required)")
    private Property<String> apiKey;

    @Schema(title = "Model Name", description = "Model name to use (e.g., gemini-1.5-flash, GPT-4, llama3)")
    private Property<String> modelName;

    @Schema(title = "Ollama Endpoint", description = "Endpoint for Ollama API (required for OLLAMA)")
    private Property<String> endpoint;
}
