package io.kestra.plugin.langchain4j.tool;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
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

import java.util.List;
import java.util.Map;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Plugin(
    beta = true,
    examples = {
        @Example(
            title = "Chat Completion with Google Gemini and an Stdio MCP Client tool",
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
                    type: io.kestra.plugin.langchain4j.ChatCompletion
                    provider:
                      type: io.kestra.plugin.langchain4j.provider.GoogleGemini
                      apiKey: "{{secret('GOOGLE_API_KEY')}}"
                      modelName: gemini-2.0-flash
                    messages:
                      - type: SYSTEM
                        content: You are a helpful assistant, answer concisely, avoid overly casual language or unnecessary verbosity.
                      - type: USER
                        content: "{{inputs.prompt}}"
                    tools:
                      - type: io.kestra.plugin.langchain4j.tool.StdioMcpClient
                        command: ["docker", "run", "--rm", "-i", "mcp/time"]
                """
            }
        ),
    }
)
@JsonDeserialize
@Schema(
    title = "Model Context Protocol (MCP) HTTP client tool"
)
public class StdioMcpClient extends ToolProvider {
    @Schema(title = "The MCP client command, as a list of command parts.")
    @NotNull
    private Property<List<String>> command;

    @Schema(title = "Environment variables")
    private Property<Map<String, String>> environment;

    @JsonIgnore
    private transient McpClient mcpClient;

    @Override
    public List<ToolSpecification> tool(RunContext runContext) throws IllegalVariableEvaluationException {
        McpTransport transport = new StdioMcpTransport.Builder()
            .command(runContext.render(command).asList(String.class))
            .environment(runContext.render(environment).asMap(String.class, String.class))
            .logEvents(true)
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
