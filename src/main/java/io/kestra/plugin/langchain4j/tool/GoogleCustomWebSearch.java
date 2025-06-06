package io.kestra.plugin.langchain4j.tool;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchTool;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
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

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Plugin(beta = true)
@JsonDeserialize
@Schema(
    title = "WebSearch tool for Google Custom Search"
)
public class GoogleCustomWebSearch extends ToolProvider {
    @Schema(title = "API Key")
    @NotNull
    private Property<String> csi;

    @Schema(title = "API Key")
    @NotNull
    private Property<String> apiKey;

    @Override
    public Object tool(RunContext runContext) throws IllegalVariableEvaluationException {
        final WebSearchEngine searchEngine = GoogleCustomWebSearchEngine.builder()
            .apiKey(runContext.render(this.apiKey).as(String.class).orElseThrow())
            .csi((runContext.render(this.csi).as(String.class).orElseThrow()))
            .build();

        return new WebSearchTool(searchEngine);
    }
}
