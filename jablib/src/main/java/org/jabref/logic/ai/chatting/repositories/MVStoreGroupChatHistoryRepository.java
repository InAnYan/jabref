package org.jabref.logic.ai.chatting.repositories;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;

import org.jspecify.annotations.NonNull;

public class MVStoreGroupChatHistoryRepository extends MVStoreBase implements GroupChatHistoryRepository {
    public MVStoreGroupChatHistoryRepository(@NonNull Path path, NotificationService dialogService) {
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
    public void addMessage(GroupAiIdentifier identifier, ChatHistoryRecordV2 chatHistoryRecord) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        map.put(chatHistoryRecord.id(), chatHistoryRecord);
    }

    @Override
    public void deleteMessage(GroupAiIdentifier identifier, String id) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        map.remove(id);
    }

    @Override
    public void clear(GroupAiIdentifier identifier) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        map.clear();
    }

    @Override
    public List<ChatHistoryRecordV2> getAllMessages(GroupAiIdentifier identifier) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        return map.values().stream().toList();
    }

    @Override
    public boolean isEmpty(GroupAiIdentifier identifier) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        return map.isEmpty();
    }

    @Override
    public int size(GroupAiIdentifier identifier) {
        Map<String, ChatHistoryRecordV2> map = openMap(identifier);
        return map.size();
    }

    private Map<String, ChatHistoryRecordV2> openMap(GroupAiIdentifier identifier) {
        return mvStore.openMap(identifier.toString());
    }
}
