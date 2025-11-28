package org.jabref.logic.ai.customimplementations.tokenization.algorithms;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.ai.util.ChatMessagesUtil;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import dev.langchain4j.data.message.ChatMessage;

public class ByWordsTokenEstimator implements TokenEstimator {
    private static final float WORD_FACTOR = 0.75f;

    @Override
    public int estimate(ChatMessage message) {
        return calculate(ChatMessagesUtil.getContent(message).orElse(""));
    }

    @Override
    public int estimate(List<? extends ChatMessage> messages) {
        String content = messages
                .stream()
                .map(ChatMessagesUtil::getContent)
                .flatMap(Optional::stream)
                .collect(Collectors.joining(" "));

        return calculate(content);
    }

    private int calculate(String content) {
        return Math.round(content.length() / WORD_FACTOR);
    }

    @Override
    public TokenEstimatorKind getKind() {
        return null;
    }
}
