package org.jabref.logic.ai.customimplementations.tokenization.algorithms;

import java.util.List;

import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

public interface TokenEstimator {
    int estimate(ChatMessage.Role role, String content);

    int estimate(List<ChatMessage> messages);

    TokenEstimatorKind getKind();
}
