package org.jabref.logic.ai.chatting.util;

import java.util.List;

import org.jabref.logic.ai.chatting.repositories.GroupChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;

public class GroupChatHistory implements ChatHistory {
    private final GroupChatHistoryRepository groupChatHistoryRepository;
    private final GroupAiIdentifier identifier;

    public GroupChatHistory(
            GroupChatHistoryRepository groupChatHistoryRepository,
            GroupAiIdentifier identifier
    ) {
        this.groupChatHistoryRepository = groupChatHistoryRepository;
        this.identifier = identifier;
    }

    @Override
    public void addMessage(ChatHistoryRecordV2 chatHistoryRecord) {
        groupChatHistoryRepository.addMessage(identifier, chatHistoryRecord);
    }

    @Override
    public void deleteMessage(String id) {
        groupChatHistoryRepository.deleteMessage(identifier, id);
    }

    @Override
    public void clear() {
        groupChatHistoryRepository.clear(identifier);
    }

    @Override
    public List<ChatHistoryRecordV2> getAllMessages() {
        return groupChatHistoryRepository.getAllMessages(identifier);
    }

    @Override
    public boolean isEmpty() {
        return groupChatHistoryRepository.isEmpty(identifier);
    }

    @Override
    public int size() {
        return groupChatHistoryRepository.size(identifier);
    }
}
