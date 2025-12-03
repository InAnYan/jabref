package org.jabref.logic.ai.embedding;

import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

public class EmbeddingAiFeature implements AiFeature {
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

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        // Nothing to listen for.
    }

    public CurrentEmbeddingModel getCurrentEmbeddingModel() {
        return currentEmbeddingModel;
    }

    @Override
    public void close() throws Exception {
        currentEmbeddingModel.close();
    }
}
