package org.jabref.logic.ai.ingestion.tasks;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.logic.ingestion.PersistentLinkedFileIngestor;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task generates embeddings for a {@link LinkedFile}.
 * It will check if embeddings were already generated.
 * And it also will store the embeddings.
 */
public class GenerateEmbeddingsTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEmbeddingsTask.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final LinkedFile linkedFile;
    private final ProgressCounter progressCounter;
    private final PersistentLinkedFileIngestor persistentLinkedFileIngestor;
    private final ReadOnlyBooleanProperty shutdownSignal;

    public GenerateEmbeddingsTask(
            FilePreferences filePreferences,
            IngestedDocumentsRepository ingestedDocumentsRepository,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter,
            BibDatabaseContext bibDatabaseContext,
            LinkedFile linkedFile,
            ReadOnlyBooleanProperty shutdownSignal
    ) {
        this.linkedFile = linkedFile;
        this.shutdownSignal = shutdownSignal;
        this.bibDatabaseContext = bibDatabaseContext;
        this.progressCounter = new ProgressCounter();

        this.persistentLinkedFileIngestor = new PersistentLinkedFileIngestor(
                filePreferences,
                ingestedDocumentsRepository,
                embeddingStore,
                embeddingModel,
                documentSplitter
        );

        configure();
    }

    private void configure() {
        showToUser(true);
        titleProperty().set(Localization.lang("Generating embeddings for file '%0'", linkedFile.getLink()));

        progressCounter.listenToAllProperties(this::updateProgress);
    }

    @Override
    public Void call() {
        LOGGER.debug("Starting embeddings generation task for file \"{}\"", linkedFile.getLink());

        try {
            LongTaskInfo longTaskInfo = new LongTaskInfo(
                    progressCounter,
                    shutdownSignal
            );

            persistentLinkedFileIngestor.ingest(
                    bibDatabaseContext,
                    longTaskInfo,
                    linkedFile
            );
        } catch (InterruptedException e) {
            LOGGER.debug("There is a embeddings generation task for file \"{}\". It will be cancelled, because user quits JabRef.", linkedFile.getLink());
        }

        LOGGER.debug("Finished embeddings generation task for file \"{}\"", linkedFile.getLink());
        progressCounter.stop();
        return null;
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage());
    }
}
