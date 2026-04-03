package org.jabref.logic.ai.rag;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.database.BibDatabaseContext;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class RagAiFeature implements AiFeature {
    private final CurrentAnswerEngine currentAnswerEngine;

    public RagAiFeature(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        this.currentAnswerEngine = new CurrentAnswerEngine(
                aiPreferences,
                filePreferences,
                embeddingModel,
                embeddingStore
        );
    }

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        // No listeners.
    }

    public CurrentAnswerEngine getCurrentAnswerEngine() {
        return currentAnswerEngine;
    }

    @Override
    public void close() {
        // Nothing to close.
    }
}
