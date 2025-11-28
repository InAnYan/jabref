package org.jabref.logic.ai.pipeline.logic.rag;

import java.util.List;

import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;

public interface AnswerEngine {
    List<RelevantInformation> process(String query, List<BibEntry> bibEntriesFilter);

    AnswerEngineKind getKind();
}
