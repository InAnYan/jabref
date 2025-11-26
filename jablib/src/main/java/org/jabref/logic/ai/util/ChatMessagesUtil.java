package org.jabref.logic.ai.util;

import java.util.Optional;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatMessagesUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessagesUtil.class);

    private ChatMessagesUtil() {
        throw new UnsupportedOperationException("unable to instantiate a utility class");
    }

    public static Optional<String> getContent(ChatMessage chatMessage) {
        switch (chatMessage) {
            case SystemMessage systemMessage -> {
                return Optional.of(systemMessage.text());
            }
            case UserMessage userMessage -> {
                return Optional.of(userMessage.singleText());
            }
            case AiMessage aiMessage -> {
                return Optional.of(aiMessage.text());
            }
            default -> {
                LOGGER.warn("Unable to get content from a message with type {}", chatMessage.type().name());
                return Optional.empty();
            }
        }
    }
}
