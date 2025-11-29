package org.jabref.logic.ai.chatting.repositories;

import java.util.List;

import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;

import dev.langchain4j.data.message.ChatMessage;

@Deprecated
public interface EntryChatHistoryRepositoryV1 extends AutoCloseable {
    List<ChatMessage> loadMessagesForEntry(BibEntryAiIdentifier identifier);

    void storeMessagesForEntry(BibEntryAiIdentifier identifier, List<ChatMessage> messages);
}
