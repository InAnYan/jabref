package org.jabref.logic.ai.ingestion;

import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.customimplementations.embeddingstores.MVStoreEmbeddingStore;
import org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.repositories.MVStoreIngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class IngestionAiFeature extends AiFeature {
    private static final String EMBEDDINGS_FILE_NAME = "embeddings.mv";
    private static final String FULLY_INGESTED_FILE_NAME = "fully-ingested.mv";

    private final MVStoreEmbeddingStore mvStoreEmbeddingStore;
    private final MVStoreIngestedDocumentsRepository mvStoreIngestedDocumentsRepository;

    private final CurrentDocumentSplitter currentDocumentSplitter;

    private final IngestionTaskAggregator ingestionTaskAggregator;
    private final EmbeddingsCleaner embeddingsCleaner;

    public IngestionAiFeature(
            AiPreferences aiPreferences,
            TaskExecutor taskExecutor,
            NotificationService notificationService
    ) {
        this.mvStoreEmbeddingStore = new MVStoreEmbeddingStore(
                Directories.getAiFilesDirectory().resolve(EMBEDDINGS_FILE_NAME),
                notificationService
        );
        this.mvStoreIngestedDocumentsRepository = new MVStoreIngestedDocumentsRepository(
                notificationService,
                Directories.getAiFilesDirectory().resolve(FULLY_INGESTED_FILE_NAME)
        );

        this.currentDocumentSplitter = new CurrentDocumentSplitter(aiPreferences);

        this.ingestionTaskAggregator = new IngestionTaskAggregator(
                taskExecutor
        );

        this.embeddingsCleaner = new EmbeddingsCleaner(
                aiPreferences,
                mvStoreEmbeddingStore,
                mvStoreIngestedDocumentsRepository
        );
    }


    public IngestionTaskAggregator getIngestionTaskAggregator() {
        return ingestionTaskAggregator;
    }

    public EmbeddingStore<TextSegment> getEmbeddingsStore() {
        return mvStoreEmbeddingStore;
    }

    public IngestedDocumentsRepository getIngestedDocumentsRepository() {
        return mvStoreIngestedDocumentsRepository;
    }

    public DocumentSplitter getCurrentDocumentSplitter() {
        return currentDocumentSplitter;
    }

    public EmbeddingsCleaner getEmbeddingsCleaner() {
        return embeddingsCleaner;
    }

    @Override
    public void close() throws Exception {
        // Nothing to close.
    }
}
