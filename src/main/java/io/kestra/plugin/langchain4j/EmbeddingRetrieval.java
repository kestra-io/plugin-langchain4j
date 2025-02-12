package io.kestra.plugin.langchain4j;

import dev.langchain4j.model.embedding.EmbeddingModel;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.dto.embedding.EmbeddingModelFactory;
import io.kestra.plugin.langchain4j.dto.embedding.ProviderEmbedding;
import io.kestra.plugin.langchain4j.dto.embedding.ProviderEmbeddingConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Plugin(
    examples = {
        @Example(
            title = "Embedding Retrieval using OpenAI",
            full = true,
            code = {
                """
                id: openai_embedding_retrieval
                namespace: company.team
                task:
                    id: embedding_retrieval
                    prompt: 'What is AI?'
                    provider:
                        type: OPENAI
                        apiKey: your_openai_api_key
                        modelName: text-embedding-3-small
                """
            }
        ),
        @Example(
            title = "Embedding Retrieval using Ollama",
            full = true,
            code = {
                """
                id: ollama_embedding_retrieval
                namespace: company.team
                task:
                    id: embedding_retrieval
                    prompt: 'Explain machine learning'
                    provider:
                        type: OLLAMA
                        modelName: example
                        endpoint: http://localhost:8000
                """
            }
        ),
        @Example(
            title = "Embedding Retrieval using Google Vertex AI",
            full = true,
            code = {
                """
                id: vertexai_embedding_retrieval
                namespace: company.team
                task:
                    id: embedding_retrieval
                    prompt: Deep learning applications
                    provider:
                        type: GOOGLE_VERTEX_AI
                        projectId: your_project_id
                        location: us-central1
                        modelName: your_model_name
                """
            }
        )
    }
)
public class EmbeddingRetrieval extends Task implements RunnableTask<EmbeddingRetrieval.Output> {

    @Schema(title = "prompt text", description = "The text to generate an embedding for")
    @NotNull
    protected Property<String> prompt;

    @Schema(title = "Provider Configuration", description = "Configuration for the provider (e.g., API key, model name, endpoint)")
    @NotNull
    private ProviderEmbeddingConfig provider;

    @Override
    public EmbeddingRetrieval.Output run(RunContext runContext) throws Exception {

        // Render input properties
        String renderedInput = runContext.render(prompt).as(String.class).orElseThrow();
        ProviderEmbedding renderedProviderType = provider.getType();
        String renderedModelName = runContext.render(provider.getModelName()).as(String.class).orElse(null);
        String renderedApiKey = runContext.render(provider.getApiKey()).as(String.class).orElse(null);
        String renderedEndpoint = runContext.render(provider.getEndpoint()).as(String.class).orElse(null);
        String renderedProjectId = runContext.render(provider.getProjectId()).as(String.class).orElse(null);
        String renderedLocation = runContext.render(provider.getLocation()).as(String.class).orElse(null);

        // Get the model
        EmbeddingModel model = EmbeddingModelFactory.createModel(renderedProviderType, renderedApiKey, renderedModelName, renderedEndpoint, renderedProjectId, renderedLocation);

        var embedding = model.embed(renderedInput);

        return Output.builder()
            .vectors(embedding.content().vectorAsList())
            .dimension(embedding.content().dimension())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Generated vectors", description = "The resulting embedding vector")
        private final List<Float> vectors;

        @Schema(title = "Generated dimension", description = "The resulting embedding dimension")
        private final Integer dimension;
    }
}
