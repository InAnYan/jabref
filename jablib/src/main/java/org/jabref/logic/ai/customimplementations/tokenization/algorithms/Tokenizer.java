package org.jabref.logic.ai.customimplementations.tokenization.algorithms;

import java.util.List;

import org.jabref.model.ai.tokenization.TokenEstimationStrategy;

import dev.langchain4j.data.message.ChatMessage;

public interface Tokenizer {
    int estimate(ChatMessage message);

    int estimate(List<? extends ChatMessage> messages);

    TokenEstimationStrategy getEstimationStrategy();
}
