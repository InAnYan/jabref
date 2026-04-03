package org.jabref.logic.ai.ingestion.logic.ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;

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
        String pathKey = path.toAbsolutePath().toString();

        Optional<Long> modTime = Optional.empty();
        boolean shouldIngest = true;

        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            long currentModificationTimeInSeconds = attributes.lastModifiedTime().to(TimeUnit.SECONDS);

            Optional<Long> ingestedModificationTimeInSeconds = ingestedDocumentsRepository.getIngestedDocumentModificationTimeInSeconds(pathKey);

            if (ingestedModificationTimeInSeconds.isEmpty()) {
                modTime = Optional.of(currentModificationTimeInSeconds);
            } else {
                if (currentModificationTimeInSeconds > ingestedModificationTimeInSeconds.get()) {
                    modTime = Optional.of(currentModificationTimeInSeconds);
                } else {
                    LOGGER.debug("No need to generate embeddings for file \"{}\", because it was already generated", pathKey);
                    shouldIngest = false;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not retrieve attributes of a file \"{}\"", pathKey, e);
            LOGGER.warn("Possibly regenerating embeddings for file \"{}\"", pathKey);
        }

        if (!shouldIngest) {
            return;
        }

        fileIngestor.ingest(metadata, path);

        if (!Thread.currentThread().isInterrupted()) {
            ingestedDocumentsRepository.markDocumentAsFullyIngested(pathKey, modTime.orElse(0L));
        }
    }
}

