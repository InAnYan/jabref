package org.jabref.logic.ai.chatting.util;

import java.util.Comparator;
import java.util.List;

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
}
