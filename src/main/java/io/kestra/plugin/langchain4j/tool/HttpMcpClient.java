package io.kestra.plugin.langchain4j.tool;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.langchain4j.domain.ToolProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Plugin(
    beta = true,
    examples = {
        @Example(
            full = true,
            code = """
                id: llm-completion
                namespace: company.team

                inputs:
                  - id: prompt
                    type: STRING

                tasks:
                  - id: text_completion
                    type: io.kestra.plugin.langchain4j.ChatCompletion
                    messages:
                      - type: USER
                        content: "{{inputs.prompt}}"
                    provider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                      modelName: gemini-2.5-flash-preview-05-20
                    tools:
                      - type: io.kestra.plugin.langchain4j.tool.HttpMcpClient
                        sseUrl: http://localhost:8080
                """
        )
    }
)
@JsonDeserialize
@Schema(
    title = "Model Context Protocol (MCP) HTTP client tool"
)
public class HttpMcpClient extends ToolProvider {
    @Schema(title = "SSE URL to the MCP server")
    @NotNull
    private Property<String> sseUrl;

    @Schema(title = "Connection timeout")
    private Property<Duration> timeout;

    @JsonIgnore
    private transient McpClient mcpClient;

    @Override
    public List<ToolSpecification> tool(RunContext runContext) throws IllegalVariableEvaluationException {
        McpTransport transport = new HttpMcpTransport.Builder()
            .sseUrl(runContext.render(sseUrl).as(String.class).orElseThrow())
            .timeout(runContext.render(timeout).as(Duration.class).orElse(null))
            .build();

        this.mcpClient = new DefaultMcpClient.Builder()
            .transport(transport)
            .build();

        return mcpClient.listTools();
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
