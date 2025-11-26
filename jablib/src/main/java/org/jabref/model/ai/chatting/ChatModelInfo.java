package org.jabref.model.ai.chatting;

import org.jabref.logic.ai.customimplementations.tokenization.algorithms.Tokenizer;

import dev.langchain4j.model.chat.ChatModel;

public record ChatModelInfo(
        ChatModel chatModel,
        Tokenizer tokenizer,
        AiProvider aiProvider,
        String name,
        int contextWindowSize
) {
}
