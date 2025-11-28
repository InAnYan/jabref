package org.jabref.logic.ai.pipeline.logic.ingestion;

import java.nio.file.Path;

import org.jabref.logic.ai.pipeline.logic.EmbeddingsCleaner;
import org.jabref.logic.ai.pipeline.logic.documentsplitting.DocumentSplitter;
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
            DocumentSplitter documentSplitter
    ) {
        this.fileIngestor = new FileIngestor(
                embeddingStore,
                embeddingModel,
                documentSplitter
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
