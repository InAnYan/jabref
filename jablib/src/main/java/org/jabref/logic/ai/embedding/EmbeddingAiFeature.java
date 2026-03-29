package org.jabref.logic.ai.embedding;

import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

public class EmbeddingAiFeature extends AiFeature {
    private final CurrentEmbeddingModel currentEmbeddingModel;

    public EmbeddingAiFeature(
            AiPreferences aiPreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor
    ) {
        this.currentEmbeddingModel = new CurrentEmbeddingModel(
                aiPreferences,
                notificationService,
                taskExecutor
        );
    }


    public CurrentEmbeddingModel getCurrentEmbeddingModel() {
        return currentEmbeddingModel;
    }

    @Override
    public void close() throws Exception {
        currentEmbeddingModel.close();
    }
}
