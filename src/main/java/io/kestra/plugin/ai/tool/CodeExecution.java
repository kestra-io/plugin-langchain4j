package io.kestra.plugin.ai.tool;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.code.judge0.Judge0JavaScriptExecutionTool;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Plugin(
    beta = true,
    examples = {
        @Example(
            title = "Chat Completion with Google Gemini and a CodeExecution tool with Judge0",
            full = true,
            code = """
                id: chat_completion_with_code_execution_tool
                namespace: company.team

                tasks:
                  - id: chat_completion
                    type: io.kestra.plugin.ai.ChatCompletion
                    provider:
                      type: io.kestra.plugin.ai.provider.GoogleGemini
                      apiKey: "{{ secret('GOOGLE_API_KEY') }}"
                      modelName: gemini-2.5-flash
                    messages:
                      - type: USER
                        content: "What is the square root of 49506838032859?"
                    tools:
                      - type: io.kestra.plugin.ai.tool.CodeExecution
                        apiKey: "{{ secret('RAPID_API_KEY') }}"
                """
        ),
    }
)
@JsonDeserialize
@Schema(
    title = "Code Execution tool with Judge0"
)
public class CodeExecution extends ToolProvider {

    @Schema(title = "Rapid API Key")
    @NotNull
    private Property<String> apiKey;

    @Override
    public Map<ToolSpecification, ToolExecutor> tool(RunContext runContext) throws IllegalVariableEvaluationException {
        return extract(new Judge0JavaScriptExecutionTool(runContext.render(this.apiKey).as(String.class).orElseThrow()));
    }
}
