package io.kestra.plugin.langchain4j.rag;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.ContainerTest;
import io.kestra.plugin.langchain4j.provider.Ollama;
import io.kestra.plugin.langchain4j.embeddings.KestraKVStore;
import io.kestra.plugin.langchain4j.tool.GoogleCustomWebSearchTool;
import io.kestra.plugin.langchain4j.tool.TavilyWebSearchTool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
class ChatCompletionTest extends ContainerTest {
    private final String GOOGLE_API_KEY = System.getenv("GOOGLE_API_KEY");
    private final String GOOGLE_CSI = System.getenv("GOOGLE_CSI_KEY");
    private final String TAVILY_API_KEY = System.getenv("TAVILY_API_KEY");

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
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(Property.ofExpression("{{ modelName }}"))
                    .endpoint(Property.ofExpression("{{ endpoint }}"))
                    .build()
            )
            .embeddings(KestraKVStore.builder().build())
            .fromDocuments(List.of(IngestDocument.InlineDocument.builder().content(Property.ofValue("It rains today")).build()))
            .build();

        IngestDocument.Output ingestOutput = ingest.run(runContext);
        assertThat(ingestOutput.getIngestedDocuments()).isEqualTo(1);

        var rag = ChatCompletion.builder()
            .chatProvider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(Property.ofExpression("{{ modelName }}"))
                    .endpoint(Property.ofExpression("{{ endpoint }}"))
                    .build()
            )
            .embeddings(KestraKVStore.builder().build())
            .prompt(Property.ofValue("How's the weather today?"))
            .build();

        var ragOutput = rag.run(runContext);
        assertThat(ragOutput.getCompletion()).isNotNull();
    }

    @EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".*")
    @EnabledIfEnvironmentVariable(named = "GOOGLE_CSI", matches = ".*")
    @Test
    void rag_givenGoogleCustomWebSearch() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "apikey", GOOGLE_API_KEY,
            "csi", GOOGLE_CSI,
            "flow", Map.of("id", "flow", "namespace", "namespace")
        ));

        var ingest = IngestDocument.builder()
            .provider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(Property.ofExpression("{{ modelName }}"))
                    .endpoint(Property.ofExpression("{{ endpoint }}"))
                    .build()
            )
            .embeddings(KestraKVStore.builder().build())
            .fromDocuments(List.of(IngestDocument.InlineDocument.builder().content(Property.ofValue("It rains today")).build()))
            .build();

        IngestDocument.Output ingestOutput = ingest.run(runContext);
        assertThat(ingestOutput.getIngestedDocuments()).isEqualTo(1);

        var rag = ChatCompletion.builder()
            .chatProvider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(Property.ofExpression("{{ modelName }}"))
                    .endpoint(Property.ofExpression("{{ endpoint }}"))
                    .build()
            )
            .embeddings(KestraKVStore.builder().build())
            .prompt(Property.ofValue("How's the weather today?"))
            .webSearchTool(GoogleCustomWebSearchTool.builder()
                .csi(Property.ofExpression("{{ csi }}"))
                .apiKey(Property.ofExpression("{{ apikey }}"))
                .build())
            .build();

        var ragOutput = rag.run(runContext);
        assertThat(ragOutput.getCompletion()).isNotNull();
    }

    @EnabledIfEnvironmentVariable(named = "TAVILY_API_KEY", matches = ".*")
    @Test
    void rag_givenTavilyWebSearch() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "apikey", TAVILY_API_KEY,
            "flow", Map.of("id", "flow", "namespace", "namespace")
        ));

        var ingest = IngestDocument.builder()
            .provider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(Property.ofExpression("{{ modelName }}"))
                    .endpoint(Property.ofExpression("{{ endpoint }}"))
                    .build()
            )
            .embeddings(KestraKVStore.builder().build())
            .fromDocuments(List.of(IngestDocument.InlineDocument.builder().content(Property.ofValue("It rains today")).build()))
            .build();

        IngestDocument.Output ingestOutput = ingest.run(runContext);
        assertThat(ingestOutput.getIngestedDocuments()).isEqualTo(1);

        var rag = ChatCompletion.builder()
            .chatProvider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(Property.ofExpression("{{ modelName }}"))
                    .endpoint(Property.ofExpression("{{ endpoint }}"))
                    .build()
            )
            .embeddings(KestraKVStore.builder().build())
            .prompt(Property.ofValue("How's the weather today?"))
            .webSearchTool(TavilyWebSearchTool.builder()
                .apiKey(Property.ofExpression("{{ apikey }}"))
                .build())
            .build();

        var ragOutput = rag.run(runContext);
        assertThat(ragOutput.getCompletion()).isNotNull();
    }
}