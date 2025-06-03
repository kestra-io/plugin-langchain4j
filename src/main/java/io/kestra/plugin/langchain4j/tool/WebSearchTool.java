package io.kestra.plugin.langchain4j.tool;

import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Plugin
@SuperBuilder(toBuilder = true)
@Getter
@NoArgsConstructor
public abstract class WebSearchTool {
    @Schema(title = "API Key")
    @NotNull
    Property<String> apiKey;
    @Schema(title = "Maximum number of results to return")
    @Nullable
    Property<Integer> maxResults;
    protected static final Integer DEFAULT_MAX_RESULTS = 3;
    public abstract WebSearchContentRetriever from(final RunContext runContext) throws IllegalVariableEvaluationException;
}
