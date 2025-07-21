package io.kestra.plugin.ai.memory;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.ai.ContainerTest;
import io.kestra.plugin.ai.embeddings.KestraKVStore;
import io.kestra.plugin.ai.provider.Ollama;
import io.kestra.plugin.ai.rag.ChatCompletion;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
class KestraKVMemoryTest extends ContainerTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testMemory() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "modelName", "tinydolphin",
            "endpoint", ollamaEndpoint,
            "flow", Map.of("id", "flow", "namespace", "namespace"),
            "labels", Map.of("system", Map.of("correlationId", IdUtils.create()))
        ));

        var rag = ChatCompletion.builder()
            .chatProvider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(Property.ofExpression("{{ modelName }}"))
                    .endpoint(Property.ofExpression("{{ endpoint }}"))
                    .build()
            )
            .embeddings(KestraKVStore.builder().build())
            .memory(KestraKVMemory.builder().build())
            .prompt(Property.ofValue("Hello, my name is John"))
            .build();

        var ragOutput = rag.run(runContext);
        assertThat(ragOutput.getCompletion()).isNotNull();

        // call it a second time, it should use the memory
        rag = ChatCompletion.builder()
            .chatProvider(
                Ollama.builder()
                    .type(Ollama.class.getName())
                    .modelName(Property.ofExpression("{{ modelName }}"))
                    .endpoint(Property.ofExpression("{{ endpoint }}"))
                    .build()
            )
            .embeddings(KestraKVStore.builder().build())
            .memory(KestraKVMemory.builder().build())
            .prompt(Property.ofValue("What's my name?"))
            .build();

        ragOutput = rag.run(runContext);
        assertThat(ragOutput.getCompletion()).isNotNull();
        assertThat(ragOutput.getCompletion()).contains("John");
    }
}