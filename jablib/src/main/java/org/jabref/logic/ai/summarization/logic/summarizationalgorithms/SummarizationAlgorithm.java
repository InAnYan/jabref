package org.jabref.logic.ai.summarization.logic.summarizationalgorithms;

import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.summarization.SummarizationAlgorithmName;

public interface SummarizationAlgorithm {
    String summarize(
            ChatModel chatModel,
            LongTaskInfo longTaskInfo,
            String text
    ) throws InterruptedException;

    SummarizationAlgorithmName getName();
}
