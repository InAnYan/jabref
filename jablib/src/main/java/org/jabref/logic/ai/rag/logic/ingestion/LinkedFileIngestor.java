package org.jabref.logic.ai.rag.logic.ingestion;

import java.nio.file.Path;

import org.jabref.logic.ai.rag.logic.EmbeddingsCleaner;
import org.jabref.logic.ai.rag.logic.documentsplitting.DocumentSplitterAlgorithm;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class LinkedFileIngestor {
    private final FileIngestor fileIngestor;

    public LinkedFileIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitterAlgorithm documentSplitterAlgorithm
    ) {
        this.fileIngestor = new FileIngestor(
                embeddingStore,
                embeddingModel,
                documentSplitterAlgorithm
        );
    }

    public void ingest(
            LongTaskInfo longTaskInfo,
            LinkedFile linkedFile,
            Path path
    ) throws InterruptedException {
        Metadata metadata = new Metadata();
        metadata.put(EmbeddingsCleaner.LINK_METADATA_KEY, linkedFile.getLink());

        fileIngestor.ingest(longTaskInfo, metadata, path);
    }
}
