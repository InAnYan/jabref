package org.jabref.logic.ai.customimplementations.llms;

import org.jabref.logic.ai.customimplementations.tokenization.algorithms.TokenEstimator;
import org.jabref.model.ai.debug.LlmStep;
import org.jabref.model.ai.debug.StepRecorder;
import org.jabref.model.ai.llm.AiProvider;

public interface ChatModel extends dev.langchain4j.model.chat.ChatModel {
    TokenEstimator getTokenizer();

    AiProvider getAiProvider();

    String getName();

    int getContextWindowSize();

    default LlmStep fillDebugStep(StepRecorder stepRecorder) {
        return LlmStep.withStepRecorder(stepRecorder, getAiProvider(), getName());
    }
}
