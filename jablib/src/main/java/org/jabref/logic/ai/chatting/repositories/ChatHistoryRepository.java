package org.jabref.logic.ai.chatting.repositories;

import java.util.List;

import org.jabref.model.ai.chatting.ChatHistoryIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;

// Represents a reposotory. For each method there is an identifier argument. For a simplified interface for 1 chat, use ChatHistoryFactory which outputs an ObservableList.
public interface ChatHistoryRepository {
    void addMessage(ChatHistoryIdentifier identifier, ChatMessage chatHistoryRecord);

    void deleteMessage(ChatHistoryIdentifier identifier, String id);

    void clear(ChatHistoryIdentifier identifier);

    List<ChatMessage> getAllMessages(ChatHistoryIdentifier identifier);

    boolean isEmpty(ChatHistoryIdentifier identifier);

    int size(ChatHistoryIdentifier identifier);
}
