package org.jabref.logic.ai.chatting.repositories;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.chatting.ChatHistoryRecordV1;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;

import dev.langchain4j.data.message.ChatMessage;
import kotlin.ranges.IntRange;

@Deprecated
public class MVStoreChatHistoryRepositoryV1 extends MVStoreBase implements EntryChatHistoryRepositoryV1, GroupChatHistoryRepositoryV1 {
    private static final String ENTRY_CHAT_HISTORY_PREFIX = "entry";
    private static final String GROUP_CHAT_HISTORY_PREFIX = "group";

    public MVStoreChatHistoryRepositoryV1(NotificationService dialogService, Path path) {
        super(path, dialogService);
    }

    @Override
    public List<ChatMessage> loadMessagesForEntry(BibEntryAiIdentifier identifier) {
        return loadMessagesFromMap(getMapForEntry(identifier));
    }

    @Override
    public void storeMessagesForEntry(BibEntryAiIdentifier identifier, List<ChatMessage> messages) {
        storeMessagesForMap(getMapForEntry(identifier), messages);
    }

    @Override
    public List<ChatMessage> loadMessagesForGroup(GroupAiIdentifier identifier) {
        return loadMessagesFromMap(getMapForGroup(identifier));
    }

    @Override
    public void storeMessagesForGroup(GroupAiIdentifier identifier, List<ChatMessage> messages) {
        storeMessagesForMap(getMapForGroup(identifier), messages);
    }

    private List<ChatMessage> loadMessagesFromMap(Map<Integer, ChatHistoryRecordV1> map) {
        return map
                .entrySet()
                // We need to check all keys, because upon deletion, there can be "holes" in the integer.
                .stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(entry -> entry.getValue().toLangchainMessage())
                .toList();
    }

    private void storeMessagesForMap(Map<Integer, ChatHistoryRecordV1> map, List<ChatMessage> messages) {
        map.clear();

        new IntRange(0, messages.size() - 1).forEach(i ->
                map.put(i, ChatHistoryRecordV1.fromLangchainMessage(messages.get(i)))
        );
    }

    private Map<Integer, ChatHistoryRecordV1> getMapForEntry(BibEntryAiIdentifier identifier) {
        return getMap(identifier.databasePath(), ENTRY_CHAT_HISTORY_PREFIX, identifier.citationKey());
    }

    private Map<Integer, ChatHistoryRecordV1> getMapForGroup(GroupAiIdentifier identifier) {
        return getMap(identifier.databasePath(), GROUP_CHAT_HISTORY_PREFIX, identifier.groupName());
    }

    private Map<Integer, ChatHistoryRecordV1> getMap(Path bibDatabasePath, String type, String name) {
        return mvStore.openMap(bibDatabasePath + "-" + type + "-" + name);
    }

    @Override
    protected String errorMessageForOpening() {
        return "An error occurred while opening chat history storage. Chat history of entries and groups will not be stored in the next session.";
    }

    @Override
    protected String errorMessageForOpeningLocalized() {
        return Localization.lang("An error occurred while opening chat history storage. Chat history of entries and groups will not be stored in the next session.");
    }
}
