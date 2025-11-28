package org.jabref.logic.ai.customimplementations.tokenization.algorithms;

import java.util.List;

import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import dev.langchain4j.data.message.ChatMessage;

public class MaximumTokenEstimator implements TokenEstimator {
    private final ByCharacterTokenEstimator byCharacterTokenizer = new ByCharacterTokenEstimator();
    private final ByWordsTokenEstimator byWordsTokenizer = new ByWordsTokenEstimator();

    @Override
    public int estimate(ChatMessage message) {
        int byWords = byWordsTokenizer.estimate(message);
        int byCharacter = byCharacterTokenizer.estimate(message);

        return calculate(byWords, byCharacter);
    }

    @Override
    public int estimate(List<? extends ChatMessage> messages) {
        int byWords = byWordsTokenizer.estimate(messages);
        int byCharacter = byCharacterTokenizer.estimate(messages);

        return calculate(byWords, byCharacter);
    }

    private int calculate(int byWords, int byCharacters) {
        return Math.max(byWords, byCharacters);
    }

    @Override
    public TokenEstimatorKind getKind() {
        return TokenEstimatorKind.MAX;
    }
}
