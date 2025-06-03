package io.kestra.plugin.langchain4j.tool;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.runners.RunContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Plugin(beta = true)
@JsonDeserialize
public class TavilyWebSearchTool extends WebSearchTool {

    @Override
    public WebSearchContentRetriever from(final RunContext runContext) throws IllegalVariableEvaluationException {
        final WebSearchEngine searchEngine = TavilyWebSearchEngine.builder()
            .apiKey(runContext.render(this.apiKey).as(String.class).orElseThrow())
            .build();
        return WebSearchContentRetriever.builder()
            .webSearchEngine(searchEngine)
                .maxResults(runContext.render(this.maxResults).as(Integer.class).orElse(DEFAULT_MAX_RESULTS))
                .build();
    }
}
