package org.jabref.logic.ai.current;

import java.util.List;

import org.jabref.logic.ai.pipeline.logic.rag.AnswerEngine;
import org.jabref.logic.ai.pipeline.logic.rag.EmbeddingsSearchAnswerEngine;
import org.jabref.logic.ai.pipeline.logic.rag.FullDocumentAnswerEngine;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;

import jakarta.annotation.Nullable;

public class CurrentAnswerEngine implements AnswerEngine {
    private final AiPreferences aiPreferences;

    @Nullable
    private AnswerEngine answerEngine = null;

    public CurrentAnswerEngine(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        update();
        configure();
    }

    private void update() {
        switch (aiPreferences.getAnswerEngineKind()) {
            case AnswerEngineKind.EMBEDDINGS_SEARCH ->
                    answerEngine = new EmbeddingsSearchAnswerEngine(
                            aiPreferences.getRagMinScore(),
                            aiPreferences.getRagMaxResultsCount()
                    );

            case FULL_DOCUMENT ->
                    answerEngine = new FullDocumentAnswerEngine();
        }
    }

    private void configure() {
        aiPreferences.customizeExpertSettingsProperty().addListener(_ -> update());
        aiPreferences.answerEngineKindProperty().addListener(_ -> update());
        aiPreferences.ragMaxResultsCountProperty().addListener(_ -> update());
        aiPreferences.ragMinScoreProperty().addListener(_ -> update());
    }

    @Override
    public List<RelevantInformation> process(String query, List<BibEntry> bibEntriesFilter) {
        if (answerEngine == null) {
            return List.of();
        } else {
            return answerEngine.process(query, bibEntriesFilter);
        }
    }

    @Override
    public AnswerEngineKind getKind() {
        return aiPreferences.getAnswerEngineKind();
    }
}
