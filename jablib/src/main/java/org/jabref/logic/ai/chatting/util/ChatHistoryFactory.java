package org.jabref.logic.ai.chatting.util;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.GroupChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;

public class ChatHistoryFactory {
    private ChatHistoryFactory() {
        throw new UnsupportedOperationException("unable to instantiate a utility class");
    }

    // Works one way: when the property is modified, the repository is modified too, but not vice versa.
    public static ListProperty<ChatHistoryRecordV2> makeChatHistoryProperty(
            BibEntryAiIdentifier identifier,
            EntryChatHistoryRepository repository
    ) {
        ObservableList<ChatHistoryRecordV2> list =
                FXCollections.observableArrayList(repository.getAllMessages(identifier));

        ListProperty<ChatHistoryRecordV2> property =
                new SimpleListProperty<>(list);

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

        return property;
    }

    // Works one way: when the property is modified, the repository is modified too, but not vice versa.
    public static ListProperty<ChatHistoryRecordV2> makeChatHistoryProperty(
            GroupAiIdentifier identifier,
            GroupChatHistoryRepository repository
    ) {
        ObservableList<ChatHistoryRecordV2> list =
                FXCollections.observableArrayList(repository.getAllMessages(identifier));

        ListProperty<ChatHistoryRecordV2> property =
                new SimpleListProperty<>(list);

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

        return property;
    }
}
