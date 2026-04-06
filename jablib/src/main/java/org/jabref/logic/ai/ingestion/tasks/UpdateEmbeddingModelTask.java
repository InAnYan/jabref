package org.jabref.logic.ai.ingestion.tasks;

import java.io.IOException;

import org.jabref.logic.ai.embedding.DeepJavaEmbeddingModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads (or verifies) the local embedding model and returns the ready-to-use
 * {@link DeepJavaEmbeddingModel}. The caller is responsible for updating any
 * property / field once the task succeeds (via {@code onSuccess}).
 */
public class UpdateEmbeddingModelTask extends BackgroundTask<DeepJavaEmbeddingModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateEmbeddingModelTask.class);

    private static final String DJL_EMBEDDING_MODEL_URL_PREFIX = "djl://ai.djl.huggingface.pytorch/";

    private final String embeddingModelName;

    private final ProgressCounter progressCounter = new ProgressCounter();

    public UpdateEmbeddingModelTask(String embeddingModelName) {
        this.embeddingModelName = embeddingModelName;

        configure();
    }

    private void configure() {
        titleProperty().set(Localization.lang("Updating local embedding model..."));
        showToUser(true);

        progressCounter.listenToAllProperties(this::updateProgress);
    }

    @Override
    public @NonNull DeepJavaEmbeddingModel call() {
        LOGGER.info("Downloading (or checking that is was already downloaded) embedding model...");

        String modelUrl = DJL_EMBEDDING_MODEL_URL_PREFIX + embeddingModelName;

        Criteria<String, float[]> criteria =
                Criteria.builder()
                        .setTypes(String.class, float[].class)
                        .optModelUrls(modelUrl)
                        .optEngine("PyTorch")
                        .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                        .optProgress(progressCounter)
                        .build();

        try {
            DeepJavaEmbeddingModel model = new DeepJavaEmbeddingModel(criteria);
            if (criteria.isDownloaded()) {
                LOGGER.info("Embedding model is already downloaded");
            } else {
                LOGGER.info("Embedding model was successfully updated");
            }
            return model;
        } catch (ModelNotFoundException e) {
            throw new RuntimeException(Localization.lang("Unable to find the embedding model by the URL %0", modelUrl), e);
        } catch (MalformedModelException e) {
            throw new RuntimeException(Localization.lang("The model by URL %0 is malformed", modelUrl), e);
        } catch (IOException e) {
            throw new RuntimeException(Localization.lang("An I/O error occurred while opening the embedding model by URL %0", modelUrl), e);
        } finally {
            progressCounter.stop();
        }
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage());
    }
}
