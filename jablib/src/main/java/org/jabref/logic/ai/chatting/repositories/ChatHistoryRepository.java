package org.jabref.logic.ai.chatting.repositories;

import java.util.List;

import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;

import dev.langchain4j.data.message.ChatMessage;

public interface ChatHistoryRepository extends AutoCloseable {
    List<ChatMessage> loadMessagesForEntry(BibEntryAiIdentifier identifier);

    void storeMessagesForEntry(BibEntryAiIdentifier identifier, List<ChatMessage> messages);

    List<ChatMessage> loadMessagesForGroup(GroupAiIdentifier identifier);

    void storeMessagesForGroup(GroupAiIdentifier identifier, List<ChatMessage> messages);
}
