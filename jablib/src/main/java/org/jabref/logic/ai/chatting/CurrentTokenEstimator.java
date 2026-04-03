package org.jabref.logic.ai.chatting;

import java.util.List;

import org.jabref.logic.ai.customimplementations.tokenization.algorithms.AverageTokenEstimator;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.ByCharacterTokenEstimator;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.ByWordsTokenEstimator;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.MaximumTokenEstimator;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.MinimumTokenEstimator;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.TokenEstimator;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import org.jspecify.annotations.Nullable;

public class CurrentTokenEstimator implements TokenEstimator {
    private final AiPreferences aiPreferences;

    @Nullable
    private TokenEstimator tokenEstimator;

    public CurrentTokenEstimator(
            AiPreferences aiPreferences
    ) {
        this.aiPreferences = aiPreferences;

        createTokenizer();
        setupListeningToPreferences();
    }

    private void setupListeningToPreferences() {
        aiPreferences.tokenEstimatorKindProperty().addListener(_ -> createTokenizer());
    }

    private void createTokenizer() {
        switch (aiPreferences.getTokenEstimatorKind()) {
            case TokenEstimatorKind.AVERAGE -> tokenEstimator = new AverageTokenEstimator();
            case TokenEstimatorKind.MAX -> tokenEstimator = new MaximumTokenEstimator();
            case TokenEstimatorKind.MIN -> tokenEstimator = new MinimumTokenEstimator();
            case TokenEstimatorKind.CHARS -> tokenEstimator = new ByCharacterTokenEstimator();
            case TokenEstimatorKind.WORDS -> tokenEstimator = new ByWordsTokenEstimator();
        }
    }

    @Override
    public int estimate(ChatMessage.Role role, String content) {
        if (tokenEstimator == null) {
            return 0;
        }

        return tokenEstimator.estimate(role, content);
    }

    @Override
    public int estimate(List<ChatMessage> messages) {
        if (tokenEstimator == null) {
            return 0;
        }

        return tokenEstimator.estimate(messages);
    }

    @Override
    public TokenEstimatorKind getKind() {
        return aiPreferences.getTokenEstimatorKind();
    }
}
