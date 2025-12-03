package org.jabref.logic.ai.ingestion;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.customimplementations.embeddingstores.MVStoreEmbeddingStore;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.repositories.MVStoreIngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class IngestionAiFeature implements AiFeature {
    private static final String EMBEDDINGS_FILE_NAME = "embeddings.mv";
    private static final String FULLY_INGESTED_FILE_NAME = "fully-ingested.mv";

    private final MVStoreEmbeddingStore mvStoreEmbeddingStore;
    private final MVStoreIngestedDocumentsRepository mvStoreIngestedDocumentsRepository;

    private final CurrentDocumentSplitter currentDocumentSplitter;

    private final IngestionService ingestionService;

    public IngestionAiFeature(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            TaskExecutor taskExecutor,
            NotificationService notificationService,
            EmbeddingModel embeddingModel,
            ReadOnlyBooleanProperty shutdownSignal
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

        this.ingestionService = new IngestionService(
                aiPreferences,
                filePreferences,
                taskExecutor,
                embeddingModel,
                mvStoreEmbeddingStore,
                currentDocumentSplitter,
                mvStoreIngestedDocumentsRepository,
                shutdownSignal
        );
    }

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        ingestionService.setupDatabase(context);
    }

    public IngestionService getIngestionService() {
        return ingestionService;
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

    @Override
    public void close() throws Exception {

    }
}
