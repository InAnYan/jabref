package org.jabref.logic.ai.rag.util;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.rag.logic.EmbeddingsSearchAnswerEngine;
import org.jabref.logic.ai.rag.logic.FullDocumentAnswerEngine;
import org.jabref.model.ai.pipeline.AnswerEngineKind;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Static factory for creating {@link AnswerEngine} instances.
 * <p>
 * All parameters are passed explicitly so this class carries no mutable state.
 */
public final class AnswerEngineFactory {
    private AnswerEngineFactory() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static AnswerEngine create(
            AnswerEngineKind kind,
            FilePreferences filePreferences,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            double ragMinScore,
            int ragMaxResultsCount
    ) {
        return switch (kind) {
            case FULL_DOCUMENT ->
                    new FullDocumentAnswerEngine(filePreferences);
            case EMBEDDINGS_SEARCH ->
                    new EmbeddingsSearchAnswerEngine(
                            embeddingModel,
                            embeddingStore,
                            ragMinScore,
                            ragMaxResultsCount
                    );
        };
    }
}
