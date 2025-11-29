package org.jabref.logic.ai.chatting.util;

import java.util.List;

import org.jabref.model.ai.chatting.ChatHistoryRecordV2;

public interface ChatHistory {
    void addMessage(ChatHistoryRecordV2 chatHistoryRecord);

    void deleteMessage(String id);

    void clear();

    List<ChatHistoryRecordV2> getAllMessages();

    boolean isEmpty();

    int size();
}
