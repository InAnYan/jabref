package org.jabref.logic.ai.pipeline.logic.rag;

import java.util.List;

import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;

public interface AnswerEngine {
    List<RelevantInformation> process(
            LongTaskInfo longTaskInfo,
            String query,
            List<FullBibEntryAiIdentifier> entriesFilter
    );

    AnswerEngineKind getKind();
}
