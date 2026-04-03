package org.jabref.logic.ai.embedding;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.ai.ingestion.tasks.UpdateEmbeddingModelTask;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around langchain4j {@link dev.langchain4j.model.embedding.EmbeddingModel}.
 * <p>
 * This class listens to preferences changes.
 */
public class CurrentEmbeddingModel implements EmbeddingModel, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentEmbeddingModel.class);

    private final AiPreferences aiPreferences;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;

    private final ExecutorService executorService = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-embedding-pool-%d").build()
    );

    private final ObjectProperty<Optional<DeepJavaEmbeddingModel>> predictorProperty = new SimpleObjectProperty<>(Optional.empty());

    // Empty if there is no error.
    private String errorWhileBuildingModel = "";

    public CurrentEmbeddingModel(
            AiPreferences aiPreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor
    ) {
        this.aiPreferences = aiPreferences;
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;

        startRebuildingTask();

        setupListeningToPreferencesChanges();
    }

    public void startRebuildingTask() {
        if (!aiPreferences.getEnableAi()) {
            return;
        }

        predictorProperty.set(Optional.empty());

        new UpdateEmbeddingModelTask(aiPreferences, predictorProperty)
                .onSuccess(isDownloaded -> {
                    if (isDownloaded != null) {
                        if (isDownloaded) {
                            LOGGER.info("Embedding model is already downloaded");
                        } else {
                            LOGGER.info("Embedding model was successfully updated");
                        }
                    }
                    errorWhileBuildingModel = "";
                })
                .onFailure(e -> {
                    LOGGER.error("An error occurred while building the embedding model", e);
                    notificationService.notify(Localization.lang("An error occurred while building the embedding model"));
                    errorWhileBuildingModel = e.getMessage() == null ? "" : e.getMessage();
                })
                .executeWith(taskExecutor);
    }

    public boolean isPresent() {
        return predictorProperty.get().isPresent();
    }

    public boolean hadErrorWhileBuildingModel() {
        return !errorWhileBuildingModel.isEmpty();
    }

    public String getErrorWhileBuildingModel() {
        return errorWhileBuildingModel;
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.enableAiProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue && predictorProperty.get().isEmpty()) {
                startRebuildingTask();
            }
        });

        aiPreferences.customizeExpertSettingsProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue && predictorProperty.get().isEmpty()) {
                startRebuildingTask();
            }
        });

        aiPreferences.embeddingModelProperty().addListener(obs -> startRebuildingTask());
    }

    @Override
    public Response<@NotNull List<Embedding>> embedAll(List<TextSegment> list) {
        if (predictorProperty.get().isEmpty()) {
            // The rationale for RuntimeException here:
            // 1. langchain4j error handling is a mess, and it uses RuntimeExceptions
            //    everywhere. Because this method implements a langchain4j interface,
            //    we follow the same "practice".
            // 2. There is no way to encode error information from the type system: nor
            //    in the result type, nor "throws" in the method signature. Actually,
            //    it's possible, but langchain4j doesn't do it.

            throw new RuntimeException(Localization.lang("Embedding model is not set up"));
        }

        return predictorProperty.get().get().embedAll(list);
    }

    @Override
    public void close() {
        executorService.shutdownNow();
        if (predictorProperty.get().isPresent()) {
            predictorProperty.get().get().close();
        }
    }
}
