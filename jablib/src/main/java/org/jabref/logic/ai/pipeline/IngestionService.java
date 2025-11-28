package org.jabref.logic.ai.pipeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.pipeline.logic.EmbeddingsCleaner;
import org.jabref.logic.ai.pipeline.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.pipeline.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.pipeline.tasks.GenerateEmbeddingsForSeveralTask;
import org.jabref.logic.ai.pipeline.tasks.GenerateEmbeddingsTask;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.processingstatus.ProcessingInfo;
import org.jabref.model.ai.processingstatus.ProcessingState;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Main class for generating embedding for files.
 * Use this class in the logic and UI.
 */
public class IngestionService {
    // We use a {@link TreeMap} here for the same reasons we use it in {@link ChatHistoryService}.
    private final TreeMap<LinkedFile, ProcessingInfo<LinkedFile, Void>> ingestionStatusMap = new TreeMap<>(Comparator.comparing(LinkedFile::getLink));

    private final List<List<LinkedFile>> listsUnderIngestion = new ArrayList<>();

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentSplitter documentSplitter;
    private final IngestedDocumentsRepository ingestedDocumentsRepository;
    private final EmbeddingsCleaner embeddingsCleaner;

    private final ReadOnlyBooleanProperty shutdownSignal;

    public IngestionService(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            TaskExecutor taskExecutor,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            DocumentSplitter documentSplitter,
            IngestedDocumentsRepository ingestedDocumentsRepository,
            ReadOnlyBooleanProperty shutdownSignal
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;

        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.documentSplitter = documentSplitter;
        this.ingestedDocumentsRepository = ingestedDocumentsRepository;
        this.embeddingsCleaner = new EmbeddingsCleaner(
                aiPreferences,
                embeddingStore,
                ingestedDocumentsRepository
        );

        this.shutdownSignal = shutdownSignal;
    }

    public void setupDatabase(BibDatabaseContext bibDatabaseContext) {
        // GC was eating the listeners, so we have to fall back to the event bus.
        bibDatabaseContext.getDatabase().registerListener(new EntriesChangedListener(bibDatabaseContext));
    }

    private class EntriesChangedListener {
        private final BibDatabaseContext bibDatabaseContext;

        public EntriesChangedListener(BibDatabaseContext bibDatabaseContext) {
            this.bibDatabaseContext = bibDatabaseContext;
        }

        @Subscribe
        public void listen(EntriesAddedEvent e) {
            e.getBibEntries().forEach(entry -> {
                if (aiPreferences.getAutoGenerateEmbeddings()) {
                    entry.getFiles().forEach(linkedFile -> ingest(linkedFile, bibDatabaseContext));
                }

                entry.registerListener(this);
            });
        }

        @Subscribe
        public void listen(FieldChangedEvent e) {
            if (e.getField() == StandardField.FILE && aiPreferences.getAutoGenerateEmbeddings()) {
                e.getBibEntry().getFiles().forEach(linkedFile -> ingest(linkedFile, bibDatabaseContext));
            }
        }
    }

    /**
     * Start ingesting of a {@link LinkedFile}, if it was not ingested.
     * This method returns a {@link ProcessingInfo} that can be used for tracking state of the ingestion.
     * Returned {@link ProcessingInfo} is related to the passed {@link LinkedFile}, so if you call this method twice
     * on the same {@link LinkedFile}, the method will return the same {@link ProcessingInfo}.
     */
    public ProcessingInfo<LinkedFile, Void> ingest(LinkedFile linkedFile, BibDatabaseContext bibDatabaseContext) {
        ProcessingInfo<LinkedFile, Void> processingInfo = getProcessingInfo(linkedFile);

        if (processingInfo.getState() == ProcessingState.STOPPED) {
            startEmbeddingsGenerationTask(linkedFile, bibDatabaseContext, processingInfo);
        }

        return processingInfo;
    }

    /**
     * Get {@link ProcessingInfo} of a {@link LinkedFile}. Initially, it is in state {@link ProcessingState#STOPPED}.
     * This method will not start ingesting. If you need to start it, use {@link IngestionService#ingest(LinkedFile, BibDatabaseContext)}.
     */
    public ProcessingInfo<LinkedFile, Void> getProcessingInfo(LinkedFile linkedFile) {
        return ingestionStatusMap.computeIfAbsent(linkedFile, file -> new ProcessingInfo<>(linkedFile, ProcessingState.STOPPED));
    }

    public List<ProcessingInfo<LinkedFile, Void>> getProcessingInfo(List<LinkedFile> linkedFiles) {
        return linkedFiles.stream().map(this::getProcessingInfo).toList();
    }

    public List<ProcessingInfo<LinkedFile, Void>> ingest(
            StringProperty groupName,
            List<LinkedFile> linkedFiles,
            BibDatabaseContext bibDatabaseContext
    ) {
        List<ProcessingInfo<LinkedFile, Void>> result = getProcessingInfo(linkedFiles);

        if (listsUnderIngestion.contains(linkedFiles)) {
            return result;
        }

        listsUnderIngestion.add(linkedFiles);

        List<ProcessingInfo<LinkedFile, Void>> needToProcess = result.stream().filter(processingInfo -> processingInfo.getState() == ProcessingState.STOPPED).toList();
        startEmbeddingsGenerationTask(groupName, needToProcess, bibDatabaseContext);

        return result;
    }

    private void startEmbeddingsGenerationTask(
            LinkedFile linkedFile,
            BibDatabaseContext bibDatabaseContext,
            ProcessingInfo<LinkedFile, Void> processingInfo
    ) {
        processingInfo.setState(ProcessingState.PROCESSING);

        new GenerateEmbeddingsTask(
                filePreferences,
                ingestedDocumentsRepository,
                embeddingStore,
                embeddingModel,
                documentSplitter,
                bibDatabaseContext,
                linkedFile,
                shutdownSignal
        )
                .showToUser(true)
                .onSuccess(v -> processingInfo.setState(ProcessingState.SUCCESS))
                .onFailure(processingInfo::setException)
                .executeWith(taskExecutor);
    }

    private void startEmbeddingsGenerationTask(StringProperty groupName, List<ProcessingInfo<LinkedFile, Void>> linkedFiles, BibDatabaseContext bibDatabaseContext) {
        linkedFiles.forEach(processingInfo -> processingInfo.setState(ProcessingState.PROCESSING));

        new GenerateEmbeddingsForSeveralTask(
                filePreferences,
                ingestedDocumentsRepository,
                embeddingStore,
                embeddingModel,
                documentSplitter,
                bibDatabaseContext,
                groupName,
                linkedFiles,
                shutdownSignal,
                taskExecutor
        )
                .executeWith(taskExecutor);
    }

    public void clearEmbeddingsFor(List<LinkedFile> linkedFiles) {
        embeddingsCleaner.clearEmbeddingsFor(linkedFiles);
        ingestionStatusMap.values().forEach(processingInfo -> processingInfo.setState(ProcessingState.STOPPED));
    }
}
