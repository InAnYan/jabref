package org.jabref.logic.ai.customimplementations.tokenization;

import java.util.List;

import org.jabref.logic.ai.customimplementations.tokenization.algorithms.AverageTokenizer;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.ByCharacterTokenizer;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.ByWordsTokenizer;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.MaximumTokenizer;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.MinimumTokenizer;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.Tokenizer;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.tokenization.TokenEstimationStrategy;

import dev.langchain4j.data.message.ChatMessage;
import org.jspecify.annotations.Nullable;

public class CurrentlySelectedTokenEstimationStrategy implements Tokenizer {
    private final AiPreferences aiPreferences;

    @Nullable
    private Tokenizer tokenizer;

    public CurrentlySelectedTokenEstimationStrategy(
            AiPreferences aiPreferences
    ) {
        this.aiPreferences = aiPreferences;

        setupListeningToPreferences();
    }

    private void setupListeningToPreferences() {
        aiPreferences.tokenEstimationStrategyProperty().addListener(_ -> {
            createTokenizer();
        });
    }

    private void createTokenizer() {
        switch (aiPreferences.getTokenEstimationStrategy()) {
            case TokenEstimationStrategy.AVERAGE -> tokenizer = new AverageTokenizer();
            case TokenEstimationStrategy.MAX -> tokenizer = new MaximumTokenizer();
            case TokenEstimationStrategy.MIN -> tokenizer = new MinimumTokenizer();
            case TokenEstimationStrategy.CHARS -> tokenizer = new ByCharacterTokenizer();
            case TokenEstimationStrategy.WORDS -> tokenizer = new ByWordsTokenizer();
        }
    }

    @Override
    public int estimate(ChatMessage message) {
        if (tokenizer == null) {
            return 0;
        }

        return tokenizer.estimate(message);
    }

    @Override
    public int estimate(List<? extends ChatMessage> messages) {
        if (tokenizer == null) {
            return 0;
        }

        return tokenizer.estimate(messages);
    }

    @Override
    public TokenEstimationStrategy getEstimationStrategy() {
        // Sadly.
        if (tokenizer == null) {
            return null;
        }

        return tokenizer.getEstimationStrategy();
    }
}
