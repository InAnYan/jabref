package org.jabref.logic.ai.ingestion.logic.ingestion;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedFileIngestor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFileIngestor.class);

    private final FilePreferences filePreferences;
    private final PersistedFileIngestor persistedFileIngestor;

    public LinkedFileIngestor(
            FilePreferences filePreferences,
            IngestedDocumentsRepository ingestedDocumentsRepository,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter
    ) {
        this.filePreferences = filePreferences;
        this.persistedFileIngestor = new PersistedFileIngestor(
                ingestedDocumentsRepository,
                embeddingStore,
                embeddingModel,
                documentSplitter
        );
    }

    public void ingest(
            BibDatabaseContext bibDatabaseContext,
            LinkedFile linkedFile
    ) throws InterruptedException {
        LOGGER.debug("Generating embeddings for file \"{}\"", linkedFile.getLink());

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\", while generating embeddings", linkedFile.getLink());
            LOGGER.debug("Unable to generate embeddings for file \"{}\", because it was not found while generating embeddings", linkedFile.getLink());
            throw new RuntimeException(Localization.lang("Could not find path for a linked file '%0' while generating embeddings.", linkedFile.getLink()));
        }

        Metadata metadata = new Metadata();
        metadata.put(EmbeddingsCleaner.LINK_METADATA_KEY, path.get().toString());

        persistedFileIngestor.ingest(metadata, path.get());
    }
}
