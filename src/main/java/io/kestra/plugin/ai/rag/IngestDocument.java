package io.kestra.plugin.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.*;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.ListUtils;
import io.kestra.plugin.ai.domain.EmbeddingStoreProvider;
import io.kestra.plugin.ai.domain.ModelProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.kestra.core.utils.Rethrow.throwConsumer;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Ingest documents into an embedding store.",
    description = "Only text documents (TXT, HTML, Markdown) are supported for now."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Ingest documents into a KV embedding store.\\nWARNING: the KV embedding store is for quick prototyping only, as it stores the embedding vectors in a K/V Store and load them all in memory.",
            code = """
                id: document-ingestion
                namespace: company.team

                tasks:
                  - id: ingest
                    type: io.kestra.plugin.ai.rag.IngestDocument
                    provider:
                      type: io.kestra.plugin.ai.provider.GoogleGemini
                      modelName: gemini-embedding-exp-03-07
                      apiKey: "{{ secret('GEMINI_API_KEY') }}"
                    embeddings:
                      type: io.kestra.plugin.ai.embeddings.KestraKVStore
                    drop: true
                    fromExternalURLs:
                      - https://raw.githubusercontent.com/kestra-io/docs/refs/heads/main/content/blogs/release-0-22.md
                """
        ),
    },
    aliases = "io.kestra.plugin.langchain4j.rag.IngestDocument"
)
public class IngestDocument extends Task implements RunnableTask<IngestDocument.Output> {
    @Schema(
        title = "Language Model Provider",
        description = "This provider must be configured with an embedding model."
    )
    @NotNull
    @PluginProperty
    private ModelProvider provider;

    @Schema(title = "Embedding Store Provider")
    @NotNull
    @PluginProperty
    private EmbeddingStoreProvider embeddings;

    @Schema(
        title = "A path inside the task working directory that contains documents to ingest",
        description = "Each document inside the directory will be ingested into the embedding store. This is recursive and protected from being path traversal (CWE-22)."
    )
    private Property<String> fromPath;

    @Schema(
        title = "A list of internal storage URIs representing documents"
    )
    @PluginProperty(internalStorageURI = true)
    private Property<List<String>> fromInternalURIs;

    @Schema(
        title = "A list of document URLs from external sources"
    )
    private Property<List<String>> fromExternalURLs;

    @Schema(
        title = "A list of inline documents"
    )
    @PluginProperty
    private List<InlineDocument> fromDocuments;

    @Schema(
        title = "Additional metadata that will be added to all ingested documents"
    )
    private Property<Map<String, String>> metadata;

    @Schema(
        title = "The document splitter"
    )
    @PluginProperty
    private DocumentSplitter documentSplitter;

    @Schema(
        title = "Whether to drop the store before ingestion - useful for testing purposes."
    )
    @Builder.Default
    private Property<Boolean> drop = Property.ofValue(Boolean.FALSE);

    @Override
    public Output run(RunContext runContext) throws Exception {
        List<Document> documents = new ArrayList<>();

        runContext.render(fromPath).as(String.class).ifPresent(path -> {
            // we restrict to documents on the working directory*
            // resolve protects from path traversal (CWE-22), see: https://cwe.mitre.org/data/definitions/22.html
            Path finalPath = runContext.workingDir().resolve(Path.of(path));
            documents.addAll(FileSystemDocumentLoader.loadDocumentsRecursively(finalPath));
        });

        ListUtils.emptyOnNull(fromDocuments).forEach(throwConsumer(inlineDocument -> {
            Map<String, Object> metadata = runContext.render(inlineDocument.metadata).asMap(String.class, Object.class);
            documents.add(Document.document(runContext.render(inlineDocument.content).as(String.class).orElseThrow(), Metadata.from(metadata)));
        }));

        runContext.render(fromInternalURIs).asList(String.class).forEach(throwConsumer(uri -> {
            try (InputStream file = runContext.storage().getFile(URI.create(uri))) {
                byte[] bytes = file.readAllBytes();
                documents.add(Document.from(new String(bytes)));
            }
        }));

        runContext.render(fromExternalURLs).asList(String.class).forEach(throwConsumer(url -> {
            documents.add(UrlDocumentLoader.load(url, new TextDocumentParser()));
        }));

        if (metadata != null) {
            Map<String, String> metadataMap = runContext.render(metadata).asMap(String.class, Object.class);
            documents.forEach(doc -> metadataMap.forEach((k, v) -> doc.metadata().put(k, v)));
        }

        var embeddingModel = provider.embeddingModel(runContext);
        var builder = EmbeddingStoreIngestor.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddings.embeddingStore(runContext, embeddingModel.dimension(), runContext.render(drop).as(Boolean.class).orElseThrow()));

        if (documentSplitter != null) {
            builder.documentSplitter(from(documentSplitter));
        }

        EmbeddingStoreIngestor ingestor = builder.build();
        IngestionResult result = ingestor.ingest(documents);

        runContext.metric(Counter.of("indexedDocuments", documents.size()));
        if (result.tokenUsage() != null) {
            if (result.tokenUsage().inputTokenCount() != null) {
                runContext.metric(Counter.of("inputTokenCount", result.tokenUsage().inputTokenCount()));
            }
            if (result.tokenUsage().outputTokenCount() != null) {
                runContext.metric(Counter.of("outputTokenCount", result.tokenUsage().outputTokenCount()));
            }
            if (result.tokenUsage().totalTokenCount() != null) {
                runContext.metric(Counter.of("totalTokenCount", result.tokenUsage().totalTokenCount()));
            }
        }

        var output = Output.builder()
            .ingestedDocuments(documents.size())
            .embeddingStoreOutputs(embeddings.outputs(runContext));

        if (result.tokenUsage() != null) {
            output = output.inputTokenCount(result.tokenUsage().inputTokenCount())
                .outputTokenCount(result.tokenUsage().outputTokenCount())
                .totalTokenCount(result.tokenUsage().totalTokenCount());
        }

        return output.build();
    }

    private dev.langchain4j.data.document.DocumentSplitter from(DocumentSplitter splitter) {
        return switch (splitter.splitter) {
            case RECURSIVE -> DocumentSplitters.recursive(splitter.getMaxSegmentSizeInChars(), splitter.getMaxOverlapSizeInChars());
            case PARAGRAPH -> new DocumentByParagraphSplitter(splitter.getMaxSegmentSizeInChars(), splitter.getMaxOverlapSizeInChars());
            case LINE -> new DocumentByLineSplitter(splitter.getMaxSegmentSizeInChars(), splitter.getMaxOverlapSizeInChars());
            case SENTENCE -> new DocumentBySentenceSplitter(splitter.getMaxSegmentSizeInChars(), splitter.getMaxOverlapSizeInChars());
            case WORD -> new DocumentByWordSplitter(splitter.getMaxSegmentSizeInChars(), splitter.getMaxOverlapSizeInChars());
        };
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InlineDocument {
        @NotNull
        @Schema(title = "The content of the document")
        private Property<String> content;

        @Schema(title = "The metadata of the document")
        private Property<Map<String, Object>> metadata;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSplitter {
        @NotNull
        @Builder.Default
        @Schema(
            title = "Title the type of the DocumentSplitter",
            description = """
                We recommend using a RECURSIVE DocumentSplitter for generic text.
                It tries to split the document into paragraphs first and fits as many paragraphs into a single TextSegment as possible.
                If some paragraphs are too long, they are recursively split into lines, then sentences, then words, and then characters until they fit into a segment."""
        )
        private Type splitter = Type.RECURSIVE;

        @NotNull
        @Schema(title = "The maximum size of the segment, it is defined in characters.")
        private Integer maxSegmentSizeInChars;

        @NotNull
        @Schema(title = "The maximum size of the overlap, it is defined in characters. Only full sentences are considered for the overlap.")
        private Integer maxOverlapSizeInChars;

        enum Type {
            @Schema(title = """
                Splits the document into paragraphs first and fits as many paragraphs into a single TextSegment as possible.
                If some paragraphs are too long, they are recursively split into lines, then sentences, then words, and then characters until they fit into a segment.""")
            RECURSIVE,

            @Schema(title = """
                Splits the provided Document into paragraphs and attempts to fit as many paragraphs as possible into a single TextSegment.
                Paragraph boundaries are detected by a minimum of two newline characters ("\\n\\n").""")
            PARAGRAPH,

            @Schema(title = """
                Splits the provided Document into lines and attempts to fit as many lines as possible into a single TextSegment.
                Line boundaries are detected by a minimum of one newline character ("\\n").""")
            LINE,

            @Schema(title = """
                Splits the provided Document into sentences and attempts to fit as many sentences as possible into a single TextSegment.
                Sentence boundaries are detected using the Apache OpenNLP library with the English sentence model.""")
            SENTENCE,

            @Schema(title = """
                Splits the provided Document into words and attempts to fit as many words as possible into a single TextSegment.
                Word boundaries are detected by a minimum of one space (" ").""")
            WORD
        }
    }

    @Getter
    @Builder
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The number of ingested documents")
        private Integer ingestedDocuments;

        @Schema(title = "The input token count")
        private Integer inputTokenCount;

        @Schema(title = "The output token count")
        private Integer outputTokenCount;

        @Schema(title = "The total token count")
        private Integer totalTokenCount;

        @Schema(title = "Additional outputs from the embedding store.")
        private Map<String, Object> embeddingStoreOutputs;
    }

}
