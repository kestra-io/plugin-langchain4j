package io.kestra.plugin.langchain4j.store;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
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
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
class ElasticsearchEmbeddingStoreTest extends ContainerTest {
    @Inject
    private RunContextFactory runContextFactory;

    private static ElasticsearchContainer elasticsearchContainer;

    @BeforeAll
    static void startElasticsearch() {
        elasticsearchContainer = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.17.2"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withExposedPorts(9200);

        elasticsearchContainer.start();
    }

    @AfterAll
    static void stopElasticsearch() {
        elasticsearchContainer.stop();
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
                ElasticsearchEmbeddingStore.builder()
                    .connection(ElasticsearchEmbeddingStore.ElasticsearchConnection.builder()
                        .hosts(List.of("http://localhost:" + elasticsearchContainer.getMappedPort(9200)))
                        .build()
                    )
                    .indexName(Property.of("embeddings"))
                    .build()
            )
            .fromDocuments(List.of(IngestDocument.InlineDocument.builder().content(Property.of("I'm Loïc")).build()))
            .build();

        IngestDocument.Output output = task.run(runContext);
        assertThat(output.getIngestedDocuments()).isEqualTo(1);
    }

}