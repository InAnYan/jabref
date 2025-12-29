package org.jabref.logic.ai.chatting.util;

import java.util.Comparator;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatHistoryIdentifier;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;

public class ChatHistoryFactory {
    private ChatHistoryFactory() {
        throw new UnsupportedOperationException("unable to instantiate a utility class");
    }

    // Works one way: when the property is modified, the repository is modified too, but not vice versa.
    public static ObservableList<ChatHistoryRecordV2> makeChatHistoryProperty(
            ChatHistoryIdentifier identifier,
            ChatHistoryRepository repository
    ) {
        List<ChatHistoryRecordV2> allMessages = repository
                .getAllMessages(identifier)
                .stream()
                .sorted(Comparator.comparing(ChatHistoryRecordV2::createdAt))
                .toList();

        ObservableList<ChatHistoryRecordV2> list = FXCollections.observableArrayList(allMessages);

        list.addListener((ListChangeListener<ChatHistoryRecordV2>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ChatHistoryRecordV2 added : change.getAddedSubList()) {
                        repository.addMessage(identifier, added);
                    }
                }
                if (change.wasRemoved()) {
                    for (ChatHistoryRecordV2 removed : change.getRemoved()) {
                        repository.deleteMessage(identifier, removed.id());
                    }
                }
            }
        });

        return list;
    }
}
