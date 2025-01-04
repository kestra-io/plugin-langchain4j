package io.kestra.plugin.langchain;

import dev.langchain4j.model.openai.OpenAiChatModel;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@KestraTest
class LangChainTextCompletionTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        // Mock RunContext to pass the prompt
        RunContext runContext = runContextFactory.of(Map.of("prompt", "What is the capital of France?"));

        // Mock OpenAiChatModel
        OpenAiChatModel mockModel = mock(OpenAiChatModel.class);
        when(mockModel.generate("What is the capital of France?")).thenReturn("The capital of France is Paris.");

        // Create the task with the mock model
        LangChainTextCompletion task = LangChainTextCompletion.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .openAiChatModel(mockModel) // Inject the mock
            .build();

        // Run the task
        LangChainTextCompletion.Output runOutput = task.run(runContext);

        // Validate the output
        assertThat(runOutput.getCompletion(), is("The capital of France is Paris."));

        // Verify the mocked model's generate method was called
        verify(mockModel, times(1)).generate("What is the capital of France?");
    }

}
