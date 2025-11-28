package org.jabref.logic.ai.customimplementations.tokenization.algorithms;

import java.util.List;

import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import dev.langchain4j.data.message.ChatMessage;

public class AverageTokenizer implements Tokenizer {
    private final ByCharacterTokenizer byCharacterTokenizer = new ByCharacterTokenizer();
    private final ByWordsTokenizer byWordsTokenizer = new ByWordsTokenizer();

    @Override
    public int estimate(ChatMessage message) {
        int byCharacter = byCharacterTokenizer.estimate(message);
        int byWords = byWordsTokenizer.estimate(message);

        return calculate(byCharacter, byWords);
    }

    @Override
    public int estimate(List<? extends ChatMessage> messages) {
        int byCharacter = byCharacterTokenizer.estimate(messages);
        int byWords = byWordsTokenizer.estimate(messages);

        return calculate(byCharacter, byWords);
    }

    private int calculate(int byCharacter, int byWords) {
        return Math.round((byWords + byCharacter) / 2.0f);
    }

    @Override
    public TokenEstimatorKind getKind() {
        return null;
    }
}
