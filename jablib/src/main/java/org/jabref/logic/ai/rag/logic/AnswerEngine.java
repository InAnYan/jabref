package org.jabref.logic.ai.rag.logic;

import java.util.List;

import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;

public interface AnswerEngine {
    List<RelevantInformation> process(
            LongTaskInfo longTaskInfo,
            String query,
            List<BibEntryAiIdentifier> entriesFilter
    );

    AnswerEngineKind getKind();
}
