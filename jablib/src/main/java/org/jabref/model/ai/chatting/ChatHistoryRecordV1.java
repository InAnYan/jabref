package org.jabref.model.ai.chatting;

import java.io.Serializable;

import org.jabref.model.ai.chatting.messages.ErrorMessage;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ChatHistoryRecordV1(String className, String content) implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryRecordV1.class);

    public static ChatHistoryRecordV1 fromLangchainMessage(ChatMessage chatMessage) {
        String className = chatMessage.getClass().getName();
        String content = getContentFromLangchainMessage(chatMessage);
        return new ChatHistoryRecordV1(className, content);
    }

    private static String getContentFromLangchainMessage(ChatMessage chatMessage) {
        String content;

        switch (chatMessage) {
            case AiMessage aiMessage ->
                    content = aiMessage.text();
            case UserMessage userMessage ->
                    content = userMessage.singleText();
            case ErrorMessage errorMessageV1 ->
                    content = errorMessageV1.getText();
            default -> {
                LOGGER.warn("ChatHistoryRecordV1 supports only AI, user. and error messages, but added message has other type: {}", chatMessage.type().name());
                return "";
            }
        }

        return content;
    }

    public ChatMessage toLangchainMessage() {
        if (className.equals(AiMessage.class.getName())) {
            return new AiMessage(content);
        } else if (className.equals(UserMessage.class.getName())) {
            return new UserMessage(content);
        } else if (className.equals(ErrorMessage.class.getName())) {
            return new ErrorMessage(content);
        } else {
            LOGGER.warn("ChatHistoryRecordV1 supports only AI and user messages, but retrieved message has other type: {}. Will treat as an AI message.", className);
            return new AiMessage(content);
        }
    }
}
