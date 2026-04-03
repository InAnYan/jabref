package org.jabref.logic.ai.ingestion.logic.ingestion;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.util.FileHasher;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistedFileIngestor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistedFileIngestor.class);

    private final IngestedDocumentsRepository ingestedDocumentsRepository;
    private final FileIngestor fileIngestor;

    public PersistedFileIngestor(
            IngestedDocumentsRepository ingestedDocumentsRepository,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter
    ) {
        this.ingestedDocumentsRepository = ingestedDocumentsRepository;
        this.fileIngestor = new FileIngestor(
                embeddingStore,
                embeddingModel,
                documentSplitter
        );
    }

    public void ingest(
            Metadata metadata,
            Path path
    ) throws InterruptedException {
        String currentFileHash;
        boolean shouldIngest = true;

        try {
            currentFileHash = FileHasher.computeHash(path);

            if (ingestedDocumentsRepository.isDocumentIngested(currentFileHash)) {
                // File has already been ingested and has not changed (same hash)
                LOGGER.debug("No need to generate embeddings for file \"{}\", because it was already ingested and has not changed", path);
                shouldIngest = false;
            }
        } catch (IOException e) {
            LOGGER.error("Could not compute hash of file \"{}\"", path, e);
            LOGGER.warn("Possibly regenerating embeddings for file \"{}\"", path);
            currentFileHash = null;
        }

        if (!shouldIngest) {
            return;
        }

        fileIngestor.ingest(metadata, path);

        if (!Thread.currentThread().isInterrupted() && currentFileHash != null) {
            ingestedDocumentsRepository.markDocumentAsFullyIngested(currentFileHash);
        }
    }
}
