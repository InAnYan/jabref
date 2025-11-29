package org.jabref.logic.ai.chatting.repositories;

import java.util.List;

import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;

public interface GroupChatHistoryRepository {
    void addMessage(GroupAiIdentifier identifier, ChatHistoryRecordV2 chatHistoryRecord);

    void deleteMessage(GroupAiIdentifier identifier, String id);

    void clear(GroupAiIdentifier identifier);

    List<ChatHistoryRecordV2> getAllMessages(GroupAiIdentifier identifier);

    boolean isEmpty(GroupAiIdentifier identifier);

    int size(GroupAiIdentifier identifier);
}
