package org.jabref.logic.ai.ingestion.logic.documentsplitting;

import java.util.stream.Stream;

import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

public class SlidingWindowDocumentSplitter implements DocumentSplitter {
    private final dev.langchain4j.data.document.DocumentSplitter langchainDocumentSplitter;

    public SlidingWindowDocumentSplitter(int chunkSize, int chunkOverlap) {
        this.langchainDocumentSplitter = DocumentSplitters.recursive(
                chunkSize,
                chunkOverlap
        );
    }

    @Override
    public Stream<String> split(LongTaskInfo longTaskInfo, String text) throws InterruptedException {
        // NOTE: Unfortunately, there is no way to handle the shutdown signal. It returns a `List` and not a stream or an iterator.
        return langchainDocumentSplitter
                .split(new DefaultDocument(text))
                .stream()
                .map(TextSegment::text);
    }

    @Override
    public DocumentSplitterKind getKind() {
        return DocumentSplitterKind.SLIDING_WINDOW;
    }
}
