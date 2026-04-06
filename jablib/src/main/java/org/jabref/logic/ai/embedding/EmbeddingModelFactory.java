package org.jabref.logic.ai.embedding;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.embeddings.EmbeddingModelEnumeration;

/**
 * Static factory for creating {@link AsyncEmbeddingModel} instances.
 */
public final class EmbeddingModelFactory {
    private EmbeddingModelFactory() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /**
     * Creates a new {@link AsyncEmbeddingModel} for the given model kind.
     * The model immediately schedules an asynchronous task to download / load the underlying DJL model.
     */
    public static AsyncEmbeddingModel create(
            EmbeddingModelEnumeration embeddingModelKind,
            NotificationService notificationService,
            TaskExecutor taskExecutor
    ) {
        return new AsyncEmbeddingModel(embeddingModelKind, notificationService, taskExecutor);
    }

    /**
     * Convenience overload that reads the embedding model kind from {@link AiPreferences}.
     */
    public static AsyncEmbeddingModel create(
            AiPreferences aiPreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor
    ) {
        return create(aiPreferences.getEmbeddingModel(), notificationService, taskExecutor);
    }
}
