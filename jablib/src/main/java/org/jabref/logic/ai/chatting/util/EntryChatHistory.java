package org.jabref.logic.ai.chatting.util;

import java.util.List;

import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;

@Deprecated
public class EntryChatHistory implements ChatHistory {
    private final EntryChatHistoryRepository entryChatHistoryRepository;
    private final BibEntryAiIdentifier identifier;

    public EntryChatHistory(
            EntryChatHistoryRepository entryChatHistoryRepository,
            BibEntryAiIdentifier identifier
    ) {
        this.entryChatHistoryRepository = entryChatHistoryRepository;
        this.identifier = identifier;
    }

    @Override
    public void addMessage(ChatHistoryRecordV2 chatHistoryRecord) {
        entryChatHistoryRepository.addMessage(identifier, chatHistoryRecord);
    }

    @Override
    public void deleteMessage(String id) {
        entryChatHistoryRepository.deleteMessage(identifier, id);
    }

    @Override
    public void clear() {
        entryChatHistoryRepository.clear(identifier);
    }

    @Override
    public List<ChatHistoryRecordV2> getAllMessages() {
        return entryChatHistoryRepository.getAllMessages(identifier);
    }

    @Override
    public boolean isEmpty() {
        return entryChatHistoryRepository.isEmpty(identifier);
    }

    @Override
    public int size() {
        return entryChatHistoryRepository.size(identifier);
    }
}
