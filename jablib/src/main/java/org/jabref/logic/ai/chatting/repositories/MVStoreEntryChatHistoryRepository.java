package org.jabref.logic.ai.chatting.repositories;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;

import org.jspecify.annotations.NonNull;

public class MVStoreEntryChatHistoryRepository extends MVStoreBase implements EntryChatHistoryRepository {
    public MVStoreEntryChatHistoryRepository(@NonNull Path path, NotificationService dialogService) {
        super(path, dialogService);
    }

    @Override
    protected String errorMessageForOpening() {
        return "An error occurred while opening chat history storage. Chat history of entries will not be stored in the next session.";
    }

    @Override
    protected String errorMessageForOpeningLocalized() {
        return Localization.lang("An error occurred while opening chat history storage. Chat history of entries will not be stored in the next session.");
    }

    @Override
    public void addMessage(BibEntryAiIdentifier identifier, ChatHistoryRecordV2 chatHistoryRecord) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        map.put(chatHistoryRecord.id(), chatHistoryRecord);
    }

    @Override
    public void deleteMessage(BibEntryAiIdentifier identifier, String id) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        map.remove(id);
    }

    @Override
    public void clear(BibEntryAiIdentifier identifier) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        map.clear();
    }

    @Override
    public List<ChatHistoryRecordV2> getAllMessages(BibEntryAiIdentifier identifier) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        return map.values().stream().toList();
    }

    @Override
    public boolean isEmpty(BibEntryAiIdentifier identifier) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        return map.isEmpty();
    }

    @Override
    public int size(BibEntryAiIdentifier identifier) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        return map.size();
    }

    private Map<String, ChatHistoryRecordV2> openMap(BibEntryAiIdentifier identifier) {
        return mvStore.openMap(identifier.toString());
    }
}
