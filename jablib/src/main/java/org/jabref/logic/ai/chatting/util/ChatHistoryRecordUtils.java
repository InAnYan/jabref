package org.jabref.logic.ai.chatting.util;

import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.messages.ErrorMessage;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatHistoryRecordUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryRecordUtils.class);

    public static ChatMessage toLangchainMessage(ChatHistoryRecordV2 record) {
            if (record.messageTypeClassName().equals(AiMessage.class.getName())) {
                return new AiMessage(record.content());
            } else if (record.messageTypeClassName().equals(UserMessage.class.getName())) {
                return new UserMessage(record.content());
            } else if (record.messageTypeClassName().equals(ErrorMessage.class.getName())) {
                return new ErrorMessage(record.content());
            } else {
                LOGGER.warn("ChatHistoryRecordV2 supports only AI and user messages, but retrieved message has other type: {}. Will treat as an AI message.", record.messageTypeClassName());
                return new AiMessage(record.content());
            }

    }
}
