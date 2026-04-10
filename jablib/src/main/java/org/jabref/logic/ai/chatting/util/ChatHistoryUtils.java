package org.jabref.logic.ai.chatting.util;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;

public class ChatHistoryUtils {
    private ChatHistoryUtils() {
        throw new UnsupportedOperationException("unable to instantiate a utility class");
    }

    public static void transferChatHistory(
            ChatHistoryRepository chatHistoryRepository,
            ChatIdentifier oldIdentifier,
            ChatIdentifier newIdentifier
    ) {
        List<ChatMessage> chatHistory = chatHistoryRepository.getAllMessages(oldIdentifier);

        chatHistoryRepository.clear(oldIdentifier);
        chatHistoryRepository.clear(newIdentifier);

        chatHistory.forEach(record -> chatHistoryRepository.addMessage(newIdentifier, record));
    }

    // Works one way: when the property is modified, the repository is modified too, but not vice versa.
    public static ObservableList<ChatMessage> makeChatHistoryProperty(
            ChatIdentifier chatIdentifier,
            ChatHistoryRepository repository
    ) {
        List<ChatMessage> allMessages = repository
                .getAllMessages(chatIdentifier)
                .stream()
                .sorted(Comparator.comparing(ChatMessage::timestamp))
                .toList();

        ObservableList<ChatMessage> list = FXCollections.observableArrayList(allMessages);

        list.addListener((ListChangeListener<ChatMessage>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ChatMessage added : change.getAddedSubList()) {
                        repository.addMessage(chatIdentifier, added);
                    }
                }
                if (change.wasRemoved()) {
                    for (ChatMessage removed : change.getRemoved()) {
                        repository.deleteMessage(chatIdentifier, removed.id());
                    }
                }
            }
        });

        return list;
    }

    /**
     * Removes the message with the specified ID from history.
     * <p>
     * Leaves a "hole" in context, but this is intended.
     */
    public static void delete(List<ChatMessage> chatHistory, String id) {
        chatHistory.removeIf(message -> Objects.equals(message.id(), id));
    }

    /**
     * Rewinds history to the point before the specified message and returns the user content to be re-sent.
     *
     * @return the content to regenerate, or null if the message was not found
     */
    public static String regenerate(List<ChatMessage> chatHistory, String id) {
        Optional<ChatMessage> recordOpt = chatHistory
                .stream()
                .filter(message -> Objects.equals(message.id(), id))
                .findFirst();

        if (recordOpt.isEmpty()) {
            return null;
        }

        ChatMessage message = recordOpt.get();
        String contentToRegenerate = message.content();
        Instant cutoffTime = message.timestamp();

        if (message.role() != ChatMessage.Role.USER) {
            int index = chatHistory.indexOf(message);
            if (index > 0) {
                ChatMessage prev = chatHistory.get(index - 1);
                if (prev.role() == ChatMessage.Role.USER) {
                    contentToRegenerate = prev.content();
                    cutoffTime = prev.timestamp();
                }
            }
        }

        final Instant finalCutoffTime = cutoffTime;
        chatHistory.removeIf(historyMessage ->
                !historyMessage.timestamp().isBefore(finalCutoffTime)
        );

        return contentToRegenerate;
    }

    /**
     * Updates the system message in the chat history.
     * If a system message already exists, it is replaced. Otherwise, a new system message is added at the beginning.
     *
     * @param chatHistory the chat history to update
     * @param newSystemMessage the new system message content
     */
    public static void updateSystemMessage(List<ChatMessage> chatHistory, String newSystemMessage) {
        // Remove existing system message if present
        chatHistory.removeIf(message -> message.role() == ChatMessage.Role.SYSTEM);

        // Add new system message at the beginning
        if (newSystemMessage != null && !newSystemMessage.isEmpty()) {
            chatHistory.addFirst(ChatMessage.systemMessage(newSystemMessage));
        }
    }
}
