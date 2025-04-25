package io.kestra.plugin.langchain4j.store;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.ContainerTest;
import io.kestra.plugin.langchain4j.IngestDocument;
import io.kestra.plugin.langchain4j.model.OllamaModelProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
class PGVectorEmbeddingStoreTest extends ContainerTest {
    @Inject
    private RunContextFactory runContextFactory;

    private static GenericContainer<?> pg;

    @BeforeAll
    static void startPGVector() {
        pg = new GenericContainer<>("pgvector/pgvector:pg17")
            .withExposedPorts(5432)
            .withEnv(Map.of("POSTGRES_USER", "kestra", "POSTGRES_PASSWORD", "kestra"));

        pg.start();
    }

    @AfterAll
    static void stopPGVector() {
        pg.stop();
    }

    @Test
    void inlineDocuments() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "flow", Map.of("id", "flow", "namespace", "namespace")
        ));

        var task = IngestDocument.builder()
            .provider(
                OllamaModelProvider.builder()
                    .type(OllamaModelProvider.class.getName())
                    .modelName(new Property<>("{{ modelName }}"))
                    .endpoint(new Property<>("{{ endpoint }}"))
                    .build()
            )
            .embeddingStore(
                PGVectorEmbeddingStore.builder()
                    .host(Property.of("localhost"))
                    .port(Property.of(pg.getMappedPort(5432)))
                    .user(Property.of("kestra"))
                    .password(Property.of("kestra"))
                    .database(Property.of("postgres"))
                    .table(Property.of("embeddings"))
                    .build()
            )
            .fromDocuments(List.of(IngestDocument.InlineDocument.builder().content(Property.of("I'm Loïc")).build()))
            .build();

        IngestDocument.Output output = task.run(runContext);
        assertThat(output.getIngestedDocuments()).isEqualTo(1);
    }

}