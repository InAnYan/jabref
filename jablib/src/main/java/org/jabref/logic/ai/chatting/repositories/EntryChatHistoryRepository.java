package org.jabref.logic.ai.chatting.repositories;

import java.util.List;

import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;

public interface EntryChatHistoryRepository {
    void addMessage(BibEntryAiIdentifier identifier, ChatHistoryRecordV2 chatHistoryRecord);

    void deleteMessage(BibEntryAiIdentifier identifier, String id);

    void clear(BibEntryAiIdentifier identifier);

    List<ChatHistoryRecordV2> getAllMessages(BibEntryAiIdentifier identifier);

    boolean isEmpty(BibEntryAiIdentifier identifier);

    int size(BibEntryAiIdentifier identifier);
}
