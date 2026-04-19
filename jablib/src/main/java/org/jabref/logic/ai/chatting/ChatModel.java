package org.jabref.logic.ai.chatting;

import org.jabref.logic.ai.tokenization.logic.TokenEstimator;
import org.jabref.model.ai.llm.AiProvider;

// [impl->feat~ai.llms~1]
public interface ChatModel extends dev.langchain4j.model.chat.ChatModel {
    TokenEstimator getTokenizer();

    AiProvider getAiProvider();

    String getName();

    int getContextWindowSize();
}
