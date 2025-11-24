package org.jabref.logic.ai.chatting.repositories;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.chatting.ErrorMessage;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import kotlin.ranges.IntRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MVStoreChatHistoryRepository extends MVStoreBase implements ChatHistoryRepository {
    private static final String ENTRY_CHAT_HISTORY_PREFIX = "entry";
    private static final String GROUP_CHAT_HISTORY_PREFIX = "group";

    private record ChatHistoryRecord(String className, String content) implements Serializable {
        private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryRecord.class);

        public static ChatHistoryRecord fromLangchainMessage(ChatMessage chatMessage) {
            String className = chatMessage.getClass().getName();
            String content = getContentFromLangchainMessage(chatMessage);
            return new ChatHistoryRecord(className, content);
        }

        private static String getContentFromLangchainMessage(ChatMessage chatMessage) {
            String content;

            switch (chatMessage) {
                case AiMessage aiMessage ->
                        content = aiMessage.text();
                case UserMessage userMessage ->
                        content = userMessage.singleText();
                case ErrorMessage errorMessage ->
                        content = errorMessage.getText();
                default -> {
                    LOGGER.warn("ChatHistoryRecord supports only AI, user. and error messages, but added message has other type: {}", chatMessage.type().name());
                    return "";
                }
            }

            return content;
        }

        public ChatMessage toLangchainMessage() {
            if (className.equals(AiMessage.class.getName())) {
                return new AiMessage(content);
            } else if (className.equals(UserMessage.class.getName())) {
                return new UserMessage(content);
            } else if (className.equals(ErrorMessage.class.getName())) {
                return new ErrorMessage(content);
            } else {
                LOGGER.warn("ChatHistoryRecord supports only AI and user messages, but retrieved message has other type: {}. Will treat as an AI message.", className);
                return new AiMessage(content);
            }
        }
    }

    public MVStoreChatHistoryRepository(NotificationService dialogService, Path path) {
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

    private List<ChatMessage> loadMessagesFromMap(Map<Integer, ChatHistoryRecord> map) {
        return map
                .entrySet()
                // We need to check all keys, because upon deletion, there can be "holes" in the integer.
                .stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(entry -> entry.getValue().toLangchainMessage())
                .toList();
    }

    private void storeMessagesForMap(Map<Integer, ChatHistoryRecord> map, List<ChatMessage> messages) {
        map.clear();

        new IntRange(0, messages.size() - 1).forEach(i ->
                map.put(i, ChatHistoryRecord.fromLangchainMessage(messages.get(i)))
        );
    }

    private Map<Integer, ChatHistoryRecord> getMapForEntry(BibEntryAiIdentifier identifier) {
        return getMap(identifier.databasePath(), ENTRY_CHAT_HISTORY_PREFIX, identifier.citationKey());
    }

    private Map<Integer, ChatHistoryRecord> getMapForGroup(GroupAiIdentifier identifier) {
        return getMap(identifier.databasePath(), GROUP_CHAT_HISTORY_PREFIX, identifier.groupName());
    }

    private Map<Integer, ChatHistoryRecord> getMap(Path bibDatabasePath, String type, String name) {
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
