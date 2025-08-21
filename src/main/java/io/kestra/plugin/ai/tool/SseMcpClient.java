package io.kestra.plugin.ai.tool;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.tool.ToolExecutor;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.ai.domain.ToolProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Plugin(
    examples = {
        @Example(
            title = "Chat Completion with Google Gemini and an HTTP MCP Client tool",
            full = true,
            code = {
                """
                id: chat_completion_with_tools
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: chat_completion_with_tools
                    type: io.kestra.plugin.ai.completion.ChatCompletion
                    provider:
                      type: io.kestra.plugin.ai.provider.GoogleGemini
                      apiKey: "{{ secret('GOOGLE_API_KEY') }}"
                      modelName: gemini-2.5-flash
                    messages:
                      - type: SYSTEM
                        content: You are a helpful assistant, answer concisely, avoid overly casual language or unnecessary verbosity.
                      - type: USER
                        content: "{{inputs.prompt}}"
                    tools:
                      - type: io.kestra.plugin.ai.tool.HttpMcpClient
                        sseUrl: http://localhost:8080
                """
            }
        ),
    },
    aliases = { "io.kestra.plugin.langchain4j.tool.HttpMcpClient", "io.kestra.plugin.ai.tool.HttpMcpClient" }
)
@JsonDeserialize
@Schema(
    title = "Model Context Protocol (MCP) SSE client tool"
)
public class SseMcpClient extends ToolProvider {
    @Schema(title = "SSE URL to the MCP server")
    @NotNull
    private Property<String> sseUrl;

    @Schema(title = "Connection timeout")
    private Property<Duration> timeout;

    @Schema(title = "Whether to log requests")
    @NotNull
    @Builder.Default
    private Property<Boolean> logRequests = Property.ofValue(false);

    @Schema(title = "Whether to log responses")
    @NotNull
    @Builder.Default
    private Property<Boolean> logResponses = Property.ofValue(false);

    @JsonIgnore
    private transient McpClient mcpClient;

    @Override
    public Map<ToolSpecification, ToolExecutor> tool(RunContext runContext) throws IllegalVariableEvaluationException {
        McpTransport transport = new HttpMcpTransport.Builder()
            .sseUrl(runContext.render(sseUrl).as(String.class).orElseThrow())
            .timeout(runContext.render(timeout).as(Duration.class).orElse(null))
            .logRequests(runContext.render(logRequests).as(Boolean.class).orElseThrow())
            .logResponses(runContext.render(logResponses).as(Boolean.class).orElseThrow())
            .build();

        this.mcpClient = new DefaultMcpClient.Builder()
            .transport(transport)
            .build();

        return mcpClient.listTools().stream().collect(Collectors.toMap(
            tool -> tool,
            tool -> new McpToolExecutor(mcpClient)
        ));
    }

    @Override
    public void close(RunContext runContext) {
        if (mcpClient != null) {
            try {
                mcpClient.close();
            } catch (Exception e) {
                runContext.logger().warn("Unable to close the MCP client", e);
            }
        }
    }
}
