package org.jabref.logic.ai.chatting.util;

import java.util.List;

import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatHistoryIdentifier;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;

public class WrappedChatHistory implements ChatHistory {
    private final ChatHistoryRepository repository;
    private final ChatHistoryIdentifier identifier;

    public WrappedChatHistory(
            ChatHistoryRepository repository,
            ChatHistoryIdentifier identifier
    ) {
        this.repository = repository;
        this.identifier = identifier;
    }

    @Override
    public void addMessage(ChatHistoryRecordV2 chatHistoryRecord) {
        repository.addMessage(identifier, chatHistoryRecord);
    }

    @Override
    public void deleteMessage(String id) {
        repository.deleteMessage(identifier, id);
    }

    @Override
    public void clear() {
        repository.clear(identifier);
    }

    @Override
    public List<ChatHistoryRecordV2> getAllMessages() {
        return repository.getAllMessages(identifier);
    }

    @Override
    public boolean isEmpty() {
        return repository.isEmpty(identifier);
    }

    @Override
    public int size() {
        return repository.size(identifier);
    }
}
