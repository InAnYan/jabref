package org.jabref.logic.ai.rag;

import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.rag.logic.EmbeddingsSearchAnswerEngine;
import org.jabref.logic.ai.rag.logic.FullDocumentAnswerEngine;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Nullable;

public class CurrentAnswerEngine implements AnswerEngine {
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Nullable
    private AnswerEngine answerEngine = null;

    public CurrentAnswerEngine(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;

        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;

        update();
        configure();
    }

    private void update() {
        switch (aiPreferences.getAnswerEngineKind()) {
            case AnswerEngineKind.EMBEDDINGS_SEARCH ->
                    answerEngine = new EmbeddingsSearchAnswerEngine(
                            embeddingModel,
                            embeddingStore,
                            aiPreferences.getRagMinScore(),
                            aiPreferences.getRagMaxResultsCount()
                    );

            case FULL_DOCUMENT ->
                    answerEngine = new FullDocumentAnswerEngine(filePreferences);
        }
    }

    private void configure() {
        aiPreferences.customizeExpertSettingsProperty().addListener(_ -> update());
        aiPreferences.answerEngineKindProperty().addListener(_ -> update());
        aiPreferences.ragMaxResultsCountProperty().addListener(_ -> update());
        aiPreferences.ragMinScoreProperty().addListener(_ -> update());
    }

    @Override
    public List<RelevantInformation> process(
            String query,
            List<FullBibEntry> entriesFilter
    ) {
        if (answerEngine == null) {
            return List.of();
        } else {
            return answerEngine.process(query, entriesFilter);
        }
    }

    @Override
    public AnswerEngineKind getKind() {
        return aiPreferences.getAnswerEngineKind();
    }
}
