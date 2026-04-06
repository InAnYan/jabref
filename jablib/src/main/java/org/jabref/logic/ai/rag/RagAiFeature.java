package org.jabref.logic.ai.rag;

import org.jabref.logic.ai.AiFeature;
import org.jabref.model.database.BibDatabaseContext;

/**
 * RAG feature. The answer engine is created on-demand via
 * {@link org.jabref.logic.ai.rag.util.AnswerEngineFactory} in the GUI layer.
 */
public class RagAiFeature implements AiFeature {
    @Override
    public void setupDatabase(BibDatabaseContext context) {
        // No listeners.
    }

    @Override
    public void close() {
        // Nothing to close.
    }
}
