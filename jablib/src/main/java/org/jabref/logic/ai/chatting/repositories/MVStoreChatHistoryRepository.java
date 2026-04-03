package org.jabref.logic.ai.chatting.repositories;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jspecify.annotations.NonNull;

public class MVStoreChatHistoryRepository extends MVStoreBase implements ChatHistoryRepository {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    public MVStoreChatHistoryRepository(@NonNull Path path, NotificationService dialogService) {
        super(path, dialogService);
    }

    @Override
    protected String errorMessageForOpening() {
        return "An error occurred while opening chat history storage. Chat history will not be stored in the next session.";
    }

    @Override
    protected String errorMessageForOpeningLocalized() {
        return Localization.lang("An error occurred while opening chat history storage. Chat history will not be stored in the next session.");
    }

    @Override
    public void addMessage(ChatIdentifier chatIdentifier, ChatMessage chatMessage) {
        Map<String, String> map = openMap(chatIdentifier);
        try {
            map.put(chatMessage.id(), OBJECT_MAPPER.writeValueAsString(chatMessage));
        } catch (JsonProcessingException e) {
            // NOTE: This is a highly not probable exception, so wrapping in try/catch and turning to a
            // RuntimeException to ignore it.
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteMessage(ChatIdentifier chatIdentifier, String id) {
        Map<String, String> map = openMap(chatIdentifier);
        map.remove(id);
    }

    @Override
    public void clear(ChatIdentifier chatIdentifier) {
        Map<String, String> map = openMap(chatIdentifier);
        map.clear();
    }

    @Override
    public List<ChatMessage> getAllMessages(ChatIdentifier chatIdentifier) {
        Map<String, String> map = openMap(chatIdentifier);

        return map.values().stream().map(s -> {
            try {
                return OBJECT_MAPPER.readValue(s, ChatMessage.class);
            } catch (JsonProcessingException e) {
                // NOTE: This is a highly not probable exception, so wrapping in try/catch and turning to a
                // RuntimeException to ignore it.
                throw new RuntimeException(e);
            }
        }).toList();
    }

    @Override
    public boolean isEmpty(ChatIdentifier chatIdentifier) {
        Map<String, String> map = openMap(chatIdentifier);
        return map.isEmpty();
    }

    @Override
    public int size(ChatIdentifier chatIdentifier) {
        Map<String, String> map = openMap(chatIdentifier);
        return map.size();
    }

    private Map<String, String> openMap(ChatIdentifier chatIdentifier) {
        String id = chatIdentifier.libraryId() + "/" + chatIdentifier.chatType() + "/" + chatIdentifier.chatName();
        return mvStore.openMap(id);
    }
}
