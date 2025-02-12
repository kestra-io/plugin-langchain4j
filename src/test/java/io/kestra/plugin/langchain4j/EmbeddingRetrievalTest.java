package io.kestra.plugin.langchain4j;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.dto.embedding.ProviderEmbedding;
import io.kestra.plugin.langchain4j.dto.embedding.ProviderEmbeddingConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


@KestraTest
public class EmbeddingRetrievalTest extends ContainerTest{
    @Inject
    private RunContextFactory runContextFactory;


    @Test
    void testEmbeddingRetrievalOpenAI() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is AI?",
            "apiKey", "demo",
            "modelName", "text-embedding-3-small"
        ));

        EmbeddingRetrieval task = EmbeddingRetrieval.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .provider(ProviderEmbeddingConfig.builder()
                .type(ProviderEmbedding.OPENAI)
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build()
            )
            .build();

        // WHEN
        EmbeddingRetrieval.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getVectors(), notNullValue());
        assertThat(runOutput.getDimension(), is(1536));
    }

    @Test
    void testEmbeddingRetrievalOllama() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is AI?",
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint
        ));

        EmbeddingRetrieval task = EmbeddingRetrieval.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .provider(ProviderEmbeddingConfig.builder()
                .type(ProviderEmbedding.OLLAMA)
                .modelName(new Property<>("{{ modelName }}"))
                .endpoint(new Property<>("{{ endpoint }}"))
                .build()
            )
            .build();

        // WHEN
        EmbeddingRetrieval.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getVectors(), notNullValue());
    }
}