package io.kestra.plugin.langchain4j;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.model.OllamaModelProvider;
import io.kestra.plugin.langchain4j.store.KvEmbeddingStore;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
class RAGTest extends ContainerTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void rag() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "flow", Map.of("id", "flow", "namespace", "namespace")
        ));

        var ingest = IngestDocument.builder()
            .provider(
                OllamaModelProvider.builder()
                    .type(OllamaModelProvider.class.getName())
                    .modelName(new Property<>("{{ modelName }}"))
                    .endpoint(new Property<>("{{ endpoint }}"))
                    .build()
            )
            .embeddingStore(KvEmbeddingStore.builder().build())
            .fromDocuments(List.of(IngestDocument.InlineDocument.builder().content(Property.of("It rains today")).build()))
            .build();

        IngestDocument.Output ingestOutput = ingest.run(runContext);
        assertThat(ingestOutput.getIngestedDocuments()).isEqualTo(1);

        var rag = RAG.builder()
            .chatModelProvider(
                OllamaModelProvider.builder()
                    .type(OllamaModelProvider.class.getName())
                    .modelName(new Property<>("{{ modelName }}"))
                    .endpoint(new Property<>("{{ endpoint }}"))
                    .build()
            )
            .embeddingStore(KvEmbeddingStore.builder().build())
            .prompt(Property.of("How's the weather today?"))
            .build();

        var ragOutput = rag.run(runContext);
        assertThat(ragOutput.getCompletion()).isNotNull();
    }
}