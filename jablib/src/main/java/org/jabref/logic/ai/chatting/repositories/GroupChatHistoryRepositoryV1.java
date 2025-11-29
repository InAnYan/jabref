package org.jabref.logic.ai.chatting.repositories;

import java.util.List;

import org.jabref.model.ai.identifiers.GroupAiIdentifier;

import dev.langchain4j.data.message.ChatMessage;

@Deprecated
public interface GroupChatHistoryRepositoryV1 extends AutoCloseable {
    List<ChatMessage> loadMessagesForGroup(GroupAiIdentifier identifier);

    void storeMessagesForGroup(GroupAiIdentifier identifier, List<ChatMessage> messages);
}
