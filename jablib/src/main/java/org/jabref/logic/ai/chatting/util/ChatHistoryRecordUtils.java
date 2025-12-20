package org.jabref.logic.ai.chatting.util;

import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.messages.ErrorMessage;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatHistoryRecordUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryRecordUtils.class);

    private ChatHistoryRecordUtils() {
        throw new UnsupportedOperationException("unable to instantiate a utility class");
    }

    public static ChatMessage convertRecordToLangchain(ChatHistoryRecordV2 record) {
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

    public static String getMessageAuthorDisplayName(String className) {
        if (className.equals(AiMessage.class.getName())) {
            return Localization.lang("AI");
        } else if (className.equals(UserMessage.class.getName())) {
            return Localization.lang("User");
        } else if (className.equals(ErrorMessage.class.getName())) {
            return Localization.lang("Error");
        } else {
            LOGGER.warn("ChatHistoryRecordV2 supports only AI and user messages, but retrieved message has other type: {}. Will treat as an AI message.", className);
            return Localization.lang("AI");
        }
    }

    public static List<ChatMessage> convertRecordsToLangchain(List<ChatHistoryRecordV2> chatHistoryRecords) {
        return chatHistoryRecords
                .stream()
                .map(ChatHistoryRecordUtils::convertRecordToLangchain)
                .toList();
    }
}
