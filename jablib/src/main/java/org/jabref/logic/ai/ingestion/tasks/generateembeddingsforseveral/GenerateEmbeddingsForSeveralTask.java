package org.jabref.logic.ai.ingestion.tasks.generateembeddingsforseveral;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javafx.beans.property.StringProperty;
import javafx.util.Pair;

import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.ai.processingstatus.ProcessingState;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task generates embeddings for several {@link LinkedFile} (typically used for groups).
 * It will check if embeddings were already generated.
 * And it also will store the embeddings.
 */
public class GenerateEmbeddingsForSeveralTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEmbeddingsForSeveralTask.class);

    private final GenerateEmbeddingsForSeveralTaskRequest request;

    private String currentFile = "";

    private final ProgressCounter progressCounter = new ProgressCounter();

    public GenerateEmbeddingsForSeveralTask(
            GenerateEmbeddingsForSeveralTaskRequest request
    ) {
        this.request = request;

        configure(request.groupName());
    }

    private void configure(StringProperty name) {
        showToUser(true);
        titleProperty().set(Localization.lang("Generating embeddings for %0", name.get()));
        name.addListener((o, oldValue, newValue) -> titleProperty().set(Localization.lang("Generating embeddings for %0", newValue)));

        progressCounter.increaseWorkMax(request.linkedFiles().size());
        progressCounter.listenToAllProperties(this::updateProgress);
        updateProgress();
    }

    @Override
    public Void call() throws ExecutionException, InterruptedException {
        LOGGER.debug("Starting embeddings generation of several files for {}", request.groupName().get());

        List<Pair<? extends Future<?>, String>> futures = new ArrayList<>();

        request
                .linkedFiles()
                .stream()
                .map(processingInfo -> {
                    processingInfo.setState(ProcessingState.PROCESSING);
                    return new Pair<>(
                            new GenerateEmbeddingsTask(
                                    new GenerateEmbeddingsTaskRequest(
                                            request.filePreferences(),
                                            request.ingestedDocumentsRepository(),
                                            request.embeddingStore(),
                                            request.embeddingModel(),
                                            request.documentSplitter(),
                                            request.bibDatabaseContext(),
                                            processingInfo.getObject(),
                                            request.shutdownSignal()
                                    )
                            )
                                    .showToUser(false)
                                    .onSuccess(v -> processingInfo.setState(ProcessingState.SUCCESS))
                                    .onFailure(processingInfo::setException)
                                    .onFinished(() -> progressCounter.increaseWorkDone(1))
                                    .executeWith(request.taskExecutor()),
                            processingInfo.getObject().getLink());
                })
                .forEach(futures::add);

        for (Pair<? extends Future<?>, String> pair : futures) {
            currentFile = pair.getValue();
            pair.getKey().get();
        }

        LOGGER.debug("Finished embeddings generation task of several files for {}", request.groupName().get());
        progressCounter.stop();
        return null;
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage() + " - " + currentFile + ", ...");
    }
}
