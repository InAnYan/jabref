package org.jabref.logic.ai.ingestion.listeners;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiDatabaseListener;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelFactory;
import org.jabref.logic.ai.ingestion.DocumentSplitterFactory;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.ObservablesHelper;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.ListUtil;

import com.google.common.eventbus.Subscribe;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class GenerateEmbeddingsAiDatabaseListener implements AiDatabaseListener {
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final IngestedDocumentsRepository ingestedDocumentsRepository;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;
    private final IngestionTaskAggregator ingestionTaskAggregator;

    private volatile AsyncEmbeddingModel embeddingModel;
    private volatile DocumentSplitter documentSplitter;

    public GenerateEmbeddingsAiDatabaseListener(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            IngestedDocumentsRepository ingestedDocumentsRepository,
            EmbeddingStore<TextSegment> embeddingStore,
            NotificationService notificationService,
            TaskExecutor taskExecutor,
            IngestionTaskAggregator ingestionTaskAggregator
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.ingestedDocumentsRepository = ingestedDocumentsRepository;
        this.embeddingStore = embeddingStore;
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;
        this.ingestionTaskAggregator = ingestionTaskAggregator;

        ObservablesHelper.subscribeToChanges(
                this::rebuildEmbeddingModel,
                aiPreferences.enableAiProperty(),
                aiPreferences.customizeExpertSettingsProperty(),
                aiPreferences.embeddingModelProperty()
        );

        ObservablesHelper.subscribeToChanges(
                this::rebuildDocumentSplitter,
                aiPreferences.customizeExpertSettingsProperty(),
                aiPreferences.documentSplitterKindProperty(),
                aiPreferences.documentSplitterChunkSizeProperty(),
                aiPreferences.documentSplitterOverlapSizeProperty()
        );
    }

    private void rebuildEmbeddingModel() {
        if (embeddingModel != null) {
            embeddingModel.close();
        }
        embeddingModel = EmbeddingModelFactory.create(aiPreferences, notificationService, taskExecutor);
    }

    private void rebuildDocumentSplitter() {
        this.documentSplitter = DocumentSplitterFactory.create(aiPreferences);
    }

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        context.getDatabase().registerListener(new EntriesChangedListener(context));
    }

    @Override
    public void close() {
        if (embeddingModel != null) {
            embeddingModel.close();
        }
    }

    private class EntriesChangedListener {
        private final BibDatabaseContext context;

        public EntriesChangedListener(BibDatabaseContext context) {
            this.context = context;
        }

        @Subscribe
        // [impl->req~ai.ingestion.automatic-trigger~1]
        public void listen(EntriesAddedEvent e) {
            if (!aiPreferences.getAutoGenerateEmbeddings()) {
                return;
            }

            ListUtil.getLinkedFiles(e.getBibEntries()).forEach(this::ingest);
        }

        @Subscribe
        public void listen(FieldChangedEvent e) {
            if (!aiPreferences.getAutoGenerateEmbeddings() || e.getField() != StandardField.FILE) {
                return;
            }

            ListUtil.getLinkedFiles(e.getBibEntries()).forEach(this::ingest);
        }

        private void ingest(LinkedFile linkedFile) {
            ingestionTaskAggregator.start(new GenerateEmbeddingsTaskRequest(
                    filePreferences,
                    ingestedDocumentsRepository,
                    embeddingStore,
                    embeddingModel,
                    documentSplitter,
                    context,
                    linkedFile
            ));
        }
    }
}
