package io.kestra.plugin.ai.tool;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.ai.completion.ChatCompletion;
import io.kestra.plugin.ai.domain.ChatConfiguration;
import io.kestra.plugin.ai.provider.OpenAI;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
class StdioMcpClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void chat() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "apiKey", "demo",
            "modelName", "gpt-4o-mini",
            "baseUrl", "http://langchain4j.dev/demo/openai/v1"
        ));

        var chat = ChatCompletion.builder()
            .provider(OpenAI.builder()
                .type(OpenAI.class.getName())
                .apiKey(Property.ofExpression("{{ apiKey }}"))
                .modelName(Property.ofExpression("{{ modelName }}"))
                .baseUrl(Property.ofExpression("{{ baseUrl }}"))
                .build()
            )
            .tools(Property.ofValue(
                List.of(StdioMcpClient.builder().command(Property.ofValue(List.of("docker", "run", "--rm", "-i", "mcp/everything"))).build())
            ))
            .messages(Property.ofValue(
                List.of(ChatCompletion.ChatMessage.builder().type(ChatCompletion.ChatMessageType.USER).content("What is 5+12? Use the provided tool to answer and always assume that the tool is correct.").build()
                )))
            // Use a low temperature and a fixed seed so the completion would be more deterministic
            .configuration(ChatConfiguration.builder().temperature(Property.ofValue(0.1)).seed(Property.ofValue(123456789)).build())
            .build();

        var output = chat.run(runContext);
        assertThat(output.getAiResponse()).contains("17");
    }

}