package io.kestra.plugin.langchain4j.rag;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.ContainerTest;
import io.kestra.plugin.langchain4j.embeddings.KestraKVStore;
import io.kestra.plugin.langchain4j.provider.Ollama;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
class SearchTest extends ContainerTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void searchFromStore() throws Exception {
        // Given
        var runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "flow", Map.of("id", "flow", "namespace", "namespace")
        ));

        var ollamaProvider = Ollama.builder()
            .type(Ollama.class.getName())
            .modelName(Property.ofExpression("{{ modelName }}"))
            .endpoint(Property.ofExpression("{{ endpoint }}"))
            .build();

        var kestraKVEmbeddingsStore = KestraKVStore.builder().build();

        var ingestDocumentTask = IngestDocument.builder()
            .provider(ollamaProvider)
            .embeddings(kestraKVEmbeddingsStore)
            .drop(Property.ofValue(true))
            .fromDocuments(
                List.of(
                    IngestDocument.InlineDocument.builder().content(Property.ofValue("Apple")).build(),
                    IngestDocument.InlineDocument.builder().content(Property.ofValue("Cherry")).build(),
                    IngestDocument.InlineDocument.builder().content(Property.ofValue("Banana")).build()
                )
            )
            .build();

        var ingestDocumentTaskOutput = ingestDocumentTask.run(runContext);
        assertThat(ingestDocumentTaskOutput.getIngestedDocuments()).isEqualTo(3);

        // When
        var searchTask = Search.builder()
            .provider(ollamaProvider)
            .embeddings(kestraKVEmbeddingsStore)
            .query(Property.ofValue("Banana"))
            .maxResults(Property.ofValue(5))
            .minScore(Property.ofValue(0.8))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        // Then
        var searchTaskOutput = searchTask.run(runContext);
        assertThat(searchTaskOutput.getResults()).isEqualTo(List.of("Banana"));
        assertThat(searchTaskOutput.getSize()).isEqualTo(1);
    }
}
