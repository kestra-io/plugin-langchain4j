package io.kestra.plugin.ai.rag;

import io.kestra.core.context.TestRunContextFactory;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.ai.ContainerTest;
import io.kestra.plugin.ai.domain.ChatConfiguration;
import io.kestra.plugin.ai.provider.Ollama;
import io.kestra.plugin.ai.embeddings.KestraKVStore;
import io.kestra.plugin.ai.retriever.GoogleCustomWebSearch;
import io.kestra.plugin.ai.retriever.TavilyWebSearch;
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
    private TestRunContextFactory runContextFactory;

    @Test
    void rag() throws Exception {
        RunContext runContext = runContextFactory.of("namespace", Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint
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
            // Use a low temperature and a fixed seed so the completion would be more deterministic
            .chatConfiguration(ChatConfiguration.builder().temperature(Property.ofValue(0.1)).seed(Property.ofValue(123456789)).build())
            .build();

        var ragOutput = rag.run(runContext);
        assertThat(ragOutput.getCompletion()).isNotNull();
    }

    @EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".*")
    @EnabledIfEnvironmentVariable(named = "GOOGLE_CSI", matches = ".*")
    @Test
    void rag_givenGoogleCustomWebSearch() throws Exception {
        RunContext runContext = runContextFactory.of("namespace", Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "apikey", GOOGLE_API_KEY,
            "csi", GOOGLE_CSI
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
            .contentRetrievers(Property.ofValue(List.of(GoogleCustomWebSearch.builder()
                .csi(Property.ofExpression("{{ csi }}"))
                .apiKey(Property.ofExpression("{{ apikey }}"))
                .build())))
            // Use a low temperature and a fixed seed so the completion would be more deterministic
            .chatConfiguration(ChatConfiguration.builder().temperature(Property.ofValue(0.1)).seed(Property.ofValue(123456789)).build())
            .build();

        var ragOutput = rag.run(runContext);
        assertThat(ragOutput.getCompletion()).isNotNull();
    }

    @EnabledIfEnvironmentVariable(named = "TAVILY_API_KEY", matches = ".*")
    @Test
    void rag_givenTavilyWebSearch() throws Exception {
        RunContext runContext = runContextFactory.of("namespace", Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "apikey", TAVILY_API_KEY
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
            .contentRetrievers(Property.ofValue(List.of(TavilyWebSearch.builder()
                .apiKey(Property.ofExpression("{{ apikey }}"))
                .build())))
            // Use a low temperature and a fixed seed so the completion would be more deterministic
            .chatConfiguration(ChatConfiguration.builder().temperature(Property.ofValue(0.1)).seed(Property.ofValue(123456789)).build())
            .build();

        var ragOutput = rag.run(runContext);
        assertThat(ragOutput.getCompletion()).isNotNull();
    }
}