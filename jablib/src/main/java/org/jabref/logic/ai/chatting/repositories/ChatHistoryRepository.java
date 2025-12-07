package org.jabref.logic.ai.chatting.repositories;

import java.util.List;

import org.jabref.model.ai.chatting.ChatHistoryIdentifier;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;

// Represents a reposotory. For each method there is an identifier argument. For a simplified interface for 1 chat, use ChatHistoryFactory which outputs an ObservableList.
public interface ChatHistoryRepository {
    void addMessage(ChatHistoryIdentifier identifier, ChatHistoryRecordV2 chatHistoryRecord);

    void deleteMessage(ChatHistoryIdentifier identifier, String id);

    void clear(ChatHistoryIdentifier identifier);

    List<ChatHistoryRecordV2> getAllMessages(ChatHistoryIdentifier identifier);

    boolean isEmpty(ChatHistoryIdentifier identifier);

    int size(ChatHistoryIdentifier identifier);
}
