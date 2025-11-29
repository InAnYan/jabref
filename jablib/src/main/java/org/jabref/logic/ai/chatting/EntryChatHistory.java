package org.jabref.logic.ai.chatting;

import java.util.List;

import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepositoryV2;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;

public class EntryChatHistory implements ChatHistory {
    private final EntryChatHistoryRepositoryV2 entryChatHistoryRepository;
    private final BibEntryAiIdentifier identifier;

    public EntryChatHistory(
            EntryChatHistoryRepositoryV2 entryChatHistoryRepository,
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
