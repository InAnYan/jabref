package org.jabref.logic.ai.pipeline.logic.ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.pipeline.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.pipeline.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentLinkedFileIngestor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentLinkedFileIngestor.class);

    private final FilePreferences filePreferences;
    private final IngestedDocumentsRepository ingestedDocumentsRepository;
    private final LinkedFileIngestor linkedFileIngestor;

    public PersistentLinkedFileIngestor(
            FilePreferences filePreferences,
            IngestedDocumentsRepository ingestedDocumentsRepository,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter
    ) {
        this.filePreferences = filePreferences;
        this.ingestedDocumentsRepository = ingestedDocumentsRepository;
        this.linkedFileIngestor = new LinkedFileIngestor(
                embeddingStore,
                embeddingModel,
                documentSplitter
        );
    }

    public void ingest(
            BibDatabaseContext bibDatabaseContext,
            LongTaskInfo longTaskInfo,
            LinkedFile linkedFile
    ) throws InterruptedException {
        // TODO: Simplify this method.
        // Rationale for RuntimeException here:
        // See org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTask.summarizeAll

        LOGGER.debug("Generating embeddings for file \"{}\"", linkedFile.getLink());

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\", while generating embeddings", linkedFile.getLink());
            LOGGER.debug("Unable to generate embeddings for file \"{}\", because it was not found while generating embeddings", linkedFile.getLink());
            throw new RuntimeException(Localization.lang("Could not find path for a linked file '%0' while generating embeddings.", linkedFile.getLink()));
        }

        Optional<Long> modTime = Optional.empty();
        boolean shouldIngest = true;

        try {
            BasicFileAttributes attributes = Files.readAttributes(path.get(), BasicFileAttributes.class);

            long currentModificationTimeInSeconds = attributes.lastModifiedTime().to(TimeUnit.SECONDS);

            Optional<Long> ingestedModificationTimeInSeconds = ingestedDocumentsRepository.getIngestedDocumentModificationTimeInSeconds(linkedFile.getLink());

            if (ingestedModificationTimeInSeconds.isEmpty()) {
                modTime = Optional.of(currentModificationTimeInSeconds);
            } else {
                if (currentModificationTimeInSeconds > ingestedModificationTimeInSeconds.get()) {
                    modTime = Optional.of(currentModificationTimeInSeconds);
                } else {
                    LOGGER.debug("No need to generate embeddings for file \"{}\", because it was already generated", linkedFile.getLink());
                    shouldIngest = false;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not retrieve attributes of a linked file \"{}\"", linkedFile.getLink(), e);
            LOGGER.warn("Possibly regenerating embeddings for linked file \"{}\"", linkedFile.getLink());
        }

        if (!shouldIngest) {
            return;
        }

        linkedFileIngestor.ingest(longTaskInfo, linkedFile, path.get());

        if (!longTaskInfo.shutdownSignal().get()) {
            ingestedDocumentsRepository.markDocumentAsFullyIngested(linkedFile.getLink(), modTime.orElse(0L));
        }
    }
}
