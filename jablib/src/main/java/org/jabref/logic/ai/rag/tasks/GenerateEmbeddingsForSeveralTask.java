package org.jabref.logic.ai.rag.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Pair;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.rag.logic.documentsplitting.DocumentSplitterAlgorithm;
import org.jabref.logic.ai.rag.repositories.FullyIngestedDocumentsRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.processingstatus.ProcessingInfo;
import org.jabref.model.ai.processingstatus.ProcessingState;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task generates embeddings for several {@link LinkedFile} (typically used for groups).
 * It will check if embeddings were already generated.
 * And it also will store the embeddings.
 */
public class GenerateEmbeddingsForSeveralTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEmbeddingsForSeveralTask.class);

    private final FilePreferences filePreferences;
    private final FullyIngestedDocumentsRepository fullyIngestedDocumentsRepository;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitterAlgorithm documentSplitterAlgorithm;
    private final BibDatabaseContext bibDatabaseContext;
    private final StringProperty groupName;
    private final List<ProcessingInfo<LinkedFile, Void>> linkedFiles;
    private final ProgressCounter progressCounter;
    private final ReadOnlyBooleanProperty shutdownSignal;
    private final TaskExecutor taskExecutor;

    private String currentFile = "";

    public GenerateEmbeddingsForSeveralTask(
            FilePreferences filePreferences,
            FullyIngestedDocumentsRepository fullyIngestedDocumentsRepository,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitterAlgorithm documentSplitterAlgorithm,
            BibDatabaseContext bibDatabaseContext,
            StringProperty groupName,
            List<ProcessingInfo<LinkedFile, Void>> linkedFiles,
            ReadOnlyBooleanProperty shutdownSignal,
            TaskExecutor taskExecutor
    ) {
        this.filePreferences = filePreferences;
        this.fullyIngestedDocumentsRepository = fullyIngestedDocumentsRepository;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.documentSplitterAlgorithm = documentSplitterAlgorithm;
        this.bibDatabaseContext = bibDatabaseContext;
        this.groupName = groupName;
        this.linkedFiles = linkedFiles;
        this.progressCounter = new ProgressCounter();
        this.shutdownSignal = shutdownSignal;
        this.taskExecutor = taskExecutor;

        configure(groupName);
    }

    private void configure(StringProperty name) {
        showToUser(true);
        titleProperty().set(Localization.lang("Generating embeddings for %0", name.get()));
        name.addListener((o, oldValue, newValue) -> titleProperty().set(Localization.lang("Generating embeddings for %0", newValue)));

        progressCounter.increaseWorkMax(linkedFiles.size());
        progressCounter.listenToAllProperties(this::updateProgress);
        updateProgress();
    }

    @Override
    public Void call() throws ExecutionException, InterruptedException {
        LOGGER.debug("Starting embeddings generation of several files for {}", groupName.get());

        List<Pair<? extends Future<?>, String>> futures = new ArrayList<>();

        linkedFiles
                .stream()
                .map(processingInfo -> {
                    processingInfo.setState(ProcessingState.PROCESSING);
                    return new Pair<>(
                            new GenerateEmbeddingsTask(
                                    filePreferences,
                                    fullyIngestedDocumentsRepository,
                                    embeddingStore,
                                    embeddingModel,
                                    documentSplitterAlgorithm,
                                    bibDatabaseContext,
                                    processingInfo.getObject(),
                                    shutdownSignal
                            )
                                    .showToUser(false)
                                    .onSuccess(v -> processingInfo.setState(ProcessingState.SUCCESS))
                                    .onFailure(processingInfo::setException)
                                    .onFinished(() -> progressCounter.increaseWorkDone(1))
                                    .executeWith(taskExecutor),
                            processingInfo.getObject().getLink());
                })
                .forEach(futures::add);

        for (Pair<? extends Future<?>, String> pair : futures) {
            currentFile = pair.getValue();
            pair.getKey().get();
        }

        LOGGER.debug("Finished embeddings generation task of several files for {}", groupName.get());
        progressCounter.stop();
        return null;
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage() + " - " + currentFile + ", ...");
    }
}
