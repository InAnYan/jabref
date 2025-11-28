package org.jabref.logic.ai.pipeline.logic.rag;

import java.util.List;

import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;

public class EmbeddingsSearchAnswerEngine implements AnswerEngine {
    private final double minimumScore;
    private final int maximumResultsCount;

    public EmbeddingsSearchAnswerEngine(double minimumScore, int maximumResultsCount) {
        this.minimumScore = minimumScore;
        this.maximumResultsCount = maximumResultsCount;
    }

    @Override
    public List<RelevantInformation> process(String query, List<BibEntry> bibEntriesFilter) {
        return List.of();
    }

    @Override
    public AnswerEngineKind getKind() {
        return AnswerEngineKind.EMBEDDINGS_SEARCH;
    }
}
