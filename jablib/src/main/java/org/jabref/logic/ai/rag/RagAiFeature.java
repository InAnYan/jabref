package org.jabref.logic.ai.rag;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.preferences.AiPreferences;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class RagAiFeature extends AiFeature {
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


    public CurrentAnswerEngine getCurrentAnswerEngine() {
        return currentAnswerEngine;
    }

    @Override
    public void close() throws Exception {
        // Nothing to close.
    }
}
