package org.jabref.logic.ai.rag.util;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.rag.logic.EmbeddingsSearchAnswerEngine;
import org.jabref.logic.ai.rag.logic.FullDocumentAnswerEngine;
import org.jabref.model.ai.pipeline.AnswerEngineKind;

public class AnswerEngineFactory {
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;

    private final AiService aiService;

    public AnswerEngineFactory(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            AiService aiService
    ) {
        this.aiService = aiService;
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
    }

    public AnswerEngine create(AnswerEngineKind kind) {
        return switch (kind) {
            case FULL_DOCUMENT ->
                    new FullDocumentAnswerEngine(filePreferences);
            case EMBEDDINGS_SEARCH ->
                    new EmbeddingsSearchAnswerEngine(
                            aiService.getEmbeddingFeature().getCurrentEmbeddingModel(),
                            aiService.getIngestionFeature().getEmbeddingsStore(),
                            aiPreferences.getRagMinScore(),
                            aiPreferences.getRagMaxResultsCount()
                    );
        };
    }
}
