package org.jabref.logic.ai.customimplementations.tokenization.algorithms;

import java.util.List;

import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import dev.langchain4j.data.message.ChatMessage;

public interface TokenEstimator {
    int estimate(ChatMessage message);

    int estimate(List<? extends ChatMessage> messages);

    TokenEstimatorKind getKind();
}
