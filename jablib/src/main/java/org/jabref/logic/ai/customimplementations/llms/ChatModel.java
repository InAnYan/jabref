package org.jabref.logic.ai.customimplementations.llms;

import org.jabref.logic.ai.customimplementations.tokenization.algorithms.Tokenizer;
import org.jabref.model.ai.chatting.AiProvider;

public interface ChatModel extends dev.langchain4j.model.chat.ChatModel {
    Tokenizer getTokenizer();
    AiProvider getAiProvider();
    String getName();
    int getContextWindowSize();
}
