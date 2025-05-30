package io.kestra.plugin.langchain4j.provider;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiImageModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.ChatConfiguration;
import io.kestra.plugin.langchain4j.domain.ModelProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Plugin(beta = true)
@JsonDeserialize
@Schema(
        title = "Azure OpenAI Model Provider"
)
public class AzureOpenAI extends ModelProvider {

    @Schema(title = "API Key")
    private Property<String> apiKey;

    @Schema(title = "API endpoint", description = "The Azure OpenAI endpoint in the format: https://{resource}.openai.azure.com/")
    @NotNull
    private Property<String> endpoint;

    @Schema(title = "API version")
    private Property<String> serviceVersion;

    @Schema(title = "Tenant ID")
    private Property<String> tenantId;

    @Schema(title = "Client ID")
    private Property<String> clientId;

    @Schema(title = "Client secret")
    private Property<String> clientSecret;

    @Override
    public ChatModel chatModel(RunContext runContext, ChatConfiguration configuration) throws IllegalVariableEvaluationException {
        if (configuration.getTopK() != null) {
            throw new IllegalArgumentException("Azure OpenAI models didn't support setting the topK");
        }

        var seed = runContext.render(configuration.getSeed()).as(Integer.class).orElse(null);

        var apiKey = runContext.render(this.apiKey).as(String.class).orElse(null);
        var tenantId = runContext.render(this.tenantId).as(String.class).orElse(null);
        var clientId = runContext.render(this.clientId).as(String.class).orElse(null);
        var clientSecret = runContext.render(this.clientSecret).as(String.class).orElse(null);

        if (apiKey != null) {
            return AzureOpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .deploymentName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
                    .endpoint(runContext.render(this.getEndpoint()).as(String.class).orElseThrow())
                    .serviceVersion(runContext.render(this.getServiceVersion()).as(String.class).orElse(null))
                    .temperature(runContext.render(configuration.getTemperature()).as(Double.class).orElse(null))
                    .topP(runContext.render(configuration.getTopP()).as(Double.class).orElse(null))
                    .seed(seed != null ? seed.longValue() : null)
                    .build();
        } else if (tenantId != null && clientId != null && clientSecret != null) {
            return AzureOpenAiChatModel.builder()
                    .tokenCredential(credentials(runContext, tenantId, clientId, clientSecret))
                    .deploymentName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
                    .endpoint(runContext.render(this.getEndpoint()).as(String.class).orElseThrow())
                    .serviceVersion(runContext.render(this.getServiceVersion()).as(String.class).orElse(null))
                    .temperature(runContext.render(configuration.getTemperature()).as(Double.class).orElse(null))
                    .topP(runContext.render(configuration.getTopP()).as(Double.class).orElse(null))
                    .seed(seed != null ? seed.longValue() : null)
                    .build();
        } else {
            throw new IllegalArgumentException("You need to set an API Key or a tenantId, clientId and clientSecret");
        }
    }

    @Override
    public ImageModel imageModel(RunContext runContext) throws IllegalVariableEvaluationException {
        var apiKey = runContext.render(this.apiKey).as(String.class).orElse(null);
        var tenantId = runContext.render(this.tenantId).as(String.class).orElse(null);
        var clientId = runContext.render(this.clientId).as(String.class).orElse(null);
        var clientSecret = runContext.render(this.clientSecret).as(String.class).orElse(null);

        if (apiKey != null) {
            return AzureOpenAiImageModel.builder()
                    .apiKey(apiKey)
                    .deploymentName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
                    .endpoint(runContext.render(this.getEndpoint()).as(String.class).orElseThrow())
                    .serviceVersion(runContext.render(this.getServiceVersion()).as(String.class).orElse(null))
                    .build();
        } else if (tenantId != null && clientId != null && clientSecret != null) {
            return AzureOpenAiImageModel.builder()
                    .tokenCredential(credentials(runContext, tenantId, clientId, clientSecret))
                    .deploymentName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
                    .endpoint(runContext.render(this.getEndpoint()).as(String.class).orElseThrow())
                    .serviceVersion(runContext.render(this.getServiceVersion()).as(String.class).orElse(null))
                    .build();
        } else {
            throw new IllegalArgumentException("You need to set an API Key or a tenantId, clientId and clientSecret");
        }
    }

    @Override
    public EmbeddingModel embeddingModel(RunContext runContext) throws IllegalVariableEvaluationException {
        var apiKey = runContext.render(this.apiKey).as(String.class).orElse(null);
        var tenantId = runContext.render(this.tenantId).as(String.class).orElse(null);
        var clientId = runContext.render(this.clientId).as(String.class).orElse(null);
        var clientSecret = runContext.render(this.clientSecret).as(String.class).orElse(null);

        if (apiKey != null) {
            return AzureOpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .deploymentName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
                    .endpoint(runContext.render(this.getEndpoint()).as(String.class).orElseThrow())
                    .serviceVersion(runContext.render(this.getServiceVersion()).as(String.class).orElse(null))
                    .build();
        } else if (tenantId != null && clientId != null && clientSecret != null) {
            return AzureOpenAiEmbeddingModel.builder()
                    .tokenCredential(credentials(runContext, tenantId, clientId, clientSecret))
                    .deploymentName(runContext.render(this.getModelName()).as(String.class).orElseThrow())
                    .endpoint(runContext.render(this.getEndpoint()).as(String.class).orElseThrow())
                    .serviceVersion(runContext.render(this.getServiceVersion()).as(String.class).orElse(null))
                    .build();
        } else {
            throw new IllegalArgumentException("You need to set an API Key or a tenantId, clientId and clientSecret");
        }
    }

    private TokenCredential credentials(RunContext runContext, String tenantId, String clientId, String clientSecret) {

        if (StringUtils.isNotBlank(clientSecret)) {
            runContext.logger().info("Authentication is using Client Secret Credentials");
            return new ClientSecretCredentialBuilder()
                    .clientId(clientId)
                    .tenantId(tenantId)
                    .clientSecret(clientSecret)
                    .build();
        }

        runContext.logger().info("Authentication is using Default Azure Credentials");
        return new DefaultAzureCredentialBuilder().tenantId(tenantId).build();
    }
}
