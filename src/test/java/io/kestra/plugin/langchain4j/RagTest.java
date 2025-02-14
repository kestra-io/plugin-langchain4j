package io.kestra.plugin.langchain4j;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.langchain4j.dto.text.ProviderConfig;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.kestra.plugin.langchain4j.dto.text.Provider.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
public class RagTest extends ContainerTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    @Value("${kestra.gemini.apikey}")
    private String apikeyTest;

    @Test
    void testRagWithOpenAI() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is AI?",
            "context", "Artificial Intelligence (AI) refers to the simulation of human intelligence in machines.",
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "modelProvider", OPENAI
        ));

        Rag task = Rag.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .context(new Property<>("{{ context }}"))
            .provider(ProviderConfig.builder()
                .type(new Property<>("{{ modelProvider }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build())
            .build();

        // WHEN
        Rag.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getResponse(), notNullValue());
        assertThat(runOutput.getResponse().toLowerCase().contains("artificial intelligence"), is(Boolean.TRUE));
    }

    @Test
    void testRagWithGemini() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is AI?",
            "context", "Artificial Intelligence (AI) refers to the simulation of human intelligence in machines.",
            "apiKey", apikeyTest,
            "modelName", "gemini-1.5-flash",
            "modelProvider", GOOGLE_GEMINI
        ));

        Rag task = Rag.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .context(new Property<>("{{ context }}"))
            .provider(ProviderConfig.builder()
                .type(new Property<>("{{ modelProvider }}"))
                .apiKey(new Property<>("{{ apiKey }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .build())
            .build();

        // WHEN
        Rag.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getResponse(), notNullValue());
    }

    @Test
    void testRagWithOllama() throws Exception {
        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "What is AI?",
            "context", "Artificial Intelligence (AI) refers to the simulation of human intelligence in machines.",
            "modelName", "tinydolphin",
            "modelProvider", OLLAMA,
            "endPoint", ollamaEndpoint
        ));

        Rag task = Rag.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .context(new Property<>("{{ context }}"))
            .provider(ProviderConfig.builder()
                .type(new Property<>("{{ modelProvider }}"))
                .modelName(new Property<>("{{ modelName }}"))
                .endPoint(new Property<>("{{ endPoint }}"))
                .build())
            .build();

        // WHEN
        Rag.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getResponse(), notNullValue());
        assertThat(runOutput.getResponse().toLowerCase().contains("artificial intelligence"), is(Boolean.TRUE));
    }
}