package org.jabref.logic.ai.embedding;

import org.jabref.logic.ai.AiFeature;
import org.jabref.model.database.BibDatabaseContext;

public class EmbeddingAiFeature implements AiFeature {
    @Override
    public void setupDatabase(BibDatabaseContext context) {
        // No listeners.
    }

    @Override
    public void close() {
        // Nothing to close.
    }
}
