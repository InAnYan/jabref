package org.jabref.model.ai.chatting;

import dev.langchain4j.model.chat.ChatModel;

public record ChatModelInfo(ChatModel chatModel, AiProvider aiProvider, String name, int contextWindowSize) {
}
