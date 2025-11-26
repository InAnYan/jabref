package org.jabref.logic.ai.summarization.logic.summarizationalgorithms;

import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.chatting.ChatModelInfo;
import org.jabref.model.ai.summarization.SummarizationAlgorithmName;

public interface SummarizationAlgorithm {
    String summarize(
            ChatModelInfo chatModelInfo,
            LongTaskInfo longTaskInfo,
            String text
    ) throws InterruptedException;

    SummarizationAlgorithmName getName();
}
