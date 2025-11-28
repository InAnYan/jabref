package org.jabref.logic.ai.pipeline.logic.rag;

import java.util.List;

import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;

public class FullDocumentAnswerEngine implements AnswerEngine {
    @Override
    public List<RelevantInformation> process(String query, List<BibEntry> bibEntriesFilter) {
        return List.of();
    }

    @Override
    public AnswerEngineKind getKind() {
        return AnswerEngineKind.FULL_DOCUMENT;
    }
}
