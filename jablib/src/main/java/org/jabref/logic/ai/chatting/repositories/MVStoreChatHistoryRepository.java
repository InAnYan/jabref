package org.jabref.logic.ai.chatting.repositories;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.chatting.ChatHistoryIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jspecify.annotations.NonNull;

public class MVStoreChatHistoryRepository extends MVStoreBase implements ChatHistoryRepository {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
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
    public void addMessage(ChatHistoryIdentifier identifier, ChatMessage chatHistoryRecord) {
        Map<String, String> map = openMap(identifier);
        try {
            map.put(chatHistoryRecord.getId(), objectMapper.writeValueAsString(chatHistoryRecord));
        } catch (JsonProcessingException e) {
            // NOTE: This is a highly not probable exception, so wrapping in try/catch and turning to a
            // RuntimeException to ignore it.
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteMessage(ChatHistoryIdentifier identifier, String id) {
        Map<String, String> map = openMap(identifier);
        map.remove(id);
    }

    @Override
    public void clear(ChatHistoryIdentifier identifier) {
        Map<String, String> map = openMap(identifier);
        map.clear();
    }

    @Override
    public List<ChatMessage> getAllMessages(ChatHistoryIdentifier identifier) {
        Map<String, String> map = openMap(identifier);

        return map.values().stream().map(s -> {
            try {
                return objectMapper.readValue(s, ChatMessage.class);
            } catch (JsonProcessingException e) {
                // NOTE: This is a highly not probable exception, so wrapping in try/catch and turning to a
                // RuntimeException to ignore it.
                throw new RuntimeException(e);
            }
        }).toList();
    }

    @Override
    public boolean isEmpty(ChatHistoryIdentifier identifier) {
        Map<String, String> map = openMap(identifier);
        return map.isEmpty();
    }

    @Override
    public int size(ChatHistoryIdentifier identifier) {
        Map<String, String> map = openMap(identifier);
        return map.size();
    }

    private Map<String, String> openMap(ChatHistoryIdentifier identifier) {
        return mvStore.openMap(identifier.toStringRepresentation());
    }
}
