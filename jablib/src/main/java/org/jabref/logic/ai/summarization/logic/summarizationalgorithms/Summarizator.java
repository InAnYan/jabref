package org.jabref.logic.ai.summarization.logic.summarizationalgorithms;

import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.summarization.SummarizatorKind;

public interface Summarizator {
    String summarize(
            ChatModel chatModel,
            LongTaskInfo longTaskInfo,
            String text
    ) throws InterruptedException;

    SummarizatorKind getName();
}
