package org.jabref.logic.ai.rag.util;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.rag.logic.EmbeddingsSearchAnswerEngine;
import org.jabref.logic.ai.rag.logic.FullDocumentAnswerEngine;
import org.jabref.model.ai.pipeline.AnswerEngineKind;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class AnswerEngineFactory {
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public AnswerEngineFactory(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public AnswerEngine create(AnswerEngineKind kind) {
        return switch (kind) {
            case FULL_DOCUMENT ->
                    new FullDocumentAnswerEngine(filePreferences);
            case EMBEDDINGS_SEARCH ->
                    new EmbeddingsSearchAnswerEngine(
                            embeddingModel,
                            embeddingStore,
                            aiPreferences.getRagMinScore(),
                            aiPreferences.getRagMaxResultsCount()
                    );
        };
    }
}
