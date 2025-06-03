package io.kestra.plugin.langchain4j.tool;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
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
public class GoogleCustomWebSearchTool extends WebSearchTool {
    @Schema(title = "API Key")
    @NotNull
    private Property<String> csi;

    @Override
    public WebSearchContentRetriever from(final RunContext runContext) throws IllegalVariableEvaluationException {
        final WebSearchEngine searchEngine = GoogleCustomWebSearchEngine.builder()
            .apiKey(runContext.render(this.apiKey).as(String.class).orElseThrow())
            .csi((runContext.render(this.csi).as(String.class).orElseThrow()))
            .build();
        return WebSearchContentRetriever.builder()
            .webSearchEngine(searchEngine)
            .maxResults(runContext.render(this.maxResults).as(Integer.class).orElse(DEFAULT_MAX_RESULTS))
            .build();
    }
}
