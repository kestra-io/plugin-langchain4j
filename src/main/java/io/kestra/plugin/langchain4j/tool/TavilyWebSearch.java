package io.kestra.plugin.langchain4j.tool;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchTool;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
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

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Plugin(
    beta = true,
    examples = {
        @Example(
            title = "Chat Completion with Google Gemini and a WebSearch tool",
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
                      modelName: gemini-2.5-flash
                    messages:
                      - type: SYSTEM
                        content: You are a helpful assistant, answer concisely, avoid overly casual language or unnecessary verbosity.
                      - type: USER
                        content: "{{inputs.prompt}}"
                    tools:
                      - type: io.kestra.plugin.langchain4j.tool.TavilyWebSearchTool
                        apiKey: "{{ secret('TAVILY_API_KEY') }}"
                """
            }
        ),
    }
)
@JsonDeserialize
@Schema(
    title = "WebSearch tool for Tavily Search"
)
public class TavilyWebSearch extends ToolProvider {
    @Schema(title = "API Key")
    @NotNull
    private Property<String> apiKey;

    @Override
    public List<ToolSpecification> tool(RunContext runContext) throws IllegalVariableEvaluationException {
        final WebSearchEngine searchEngine = TavilyWebSearchEngine.builder()
            .apiKey(runContext.render(this.apiKey).as(String.class).orElseThrow())
            .build();

        return ToolSpecifications.toolSpecificationsFrom(new WebSearchTool(searchEngine));
    }
}
