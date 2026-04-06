package org.jabref.logic.ai.ingestion;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.embedding.MVStoreEmbeddingStore;
import org.jabref.logic.ai.ingestion.listeners.GenerateEmbeddingsAiDatabaseListener;
import org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.repositories.MVStoreIngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class IngestionAiFeature implements AiFeature {
    private static final String EMBEDDINGS_FILE_NAME = "embeddings.mv";
    private static final String FULLY_INGESTED_FILE_NAME = "fully-ingested.mv";

    private final MVStoreEmbeddingStore mvStoreEmbeddingStore;
    private final MVStoreIngestedDocumentsRepository mvStoreIngestedDocumentsRepository;

    private final IngestionTaskAggregator ingestionTaskAggregator;
    private final EmbeddingsCleaner embeddingsCleaner;

    private final GenerateEmbeddingsAiDatabaseListener generateEmbeddingsAiDatabaseListener;

    public IngestionAiFeature(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
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

        this.ingestionTaskAggregator = new IngestionTaskAggregator(
                taskExecutor
        );

        this.embeddingsCleaner = new EmbeddingsCleaner(
                aiPreferences,
                mvStoreEmbeddingStore,
                mvStoreIngestedDocumentsRepository
        );

        this.generateEmbeddingsAiDatabaseListener = new GenerateEmbeddingsAiDatabaseListener(
                aiPreferences,
                filePreferences,
                mvStoreIngestedDocumentsRepository,
                mvStoreEmbeddingStore,
                notificationService,
                taskExecutor,
                ingestionTaskAggregator
        );
    }

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        generateEmbeddingsAiDatabaseListener.setupDatabase(context);
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

    public EmbeddingsCleaner getEmbeddingsCleaner() {
        return embeddingsCleaner;
    }

    @Override
    public void close() throws Exception {
        generateEmbeddingsAiDatabaseListener.close();
    }
}
