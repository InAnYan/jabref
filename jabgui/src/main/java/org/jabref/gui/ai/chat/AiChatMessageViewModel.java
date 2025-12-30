package org.jabref.gui.ai.chat;

import java.time.Instant;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.ai.chatting.util.ChatHistoryRecordUtils;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;

public class AiChatMessageViewModel extends AbstractViewModel {
    // The chat message that is assigned by other code.
    private final ObjectProperty<ChatHistoryRecordV2> chatMessage = new SimpleObjectProperty<>();

    // Dissected properties of a chat message. This is useful for the UI but must not be changed.
    private final StringProperty id = new SimpleStringProperty("");
    private final StringProperty source = new SimpleStringProperty("");
    private final StringProperty messageContent = new SimpleStringProperty("");
    private final ObjectProperty<Instant> timestamp = new SimpleObjectProperty<>(Instant.now());

    // Actions on the chat message.
    private final ObjectProperty<EventHandler<ActionEvent>> onDelete = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerate = new SimpleObjectProperty<>();

    public AiChatMessageViewModel() {
        setupListeners();
    }

    private void setupListeners() {
        id.bind(chatMessage.map(ChatHistoryRecordV2::id));
        source.bind(chatMessage
                .map(ChatHistoryRecordV2::messageTypeClassName)
                .map(ChatHistoryRecordUtils::getMessageAuthorDisplayName));
        messageContent.bind(chatMessage.map(ChatHistoryRecordV2::content));
        timestamp.bind(chatMessage.map(ChatHistoryRecordV2::createdAt));
    }

    public void delete() {
        if (onDelete.get() != null) {
            onDelete.get().handle(new ActionEvent());
        }
    }

    public void regenerate() {
        if (onRegenerate.get() != null) {
            onRegenerate.get().handle(new ActionEvent());
        }
    }

    public ObjectProperty<ChatHistoryRecordV2> chatMessageProperty() {
        return chatMessage;
    }

    public ReadOnlyStringProperty idProperty() {
        return id;
    }

    public ReadOnlyStringProperty sourceProperty() {
        return source;
    }

    public ReadOnlyStringProperty messageContentProperty() {
        return messageContent;
    }

    public ReadOnlyObjectProperty<Instant> timestampProperty() {
        return timestamp;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onDeleteProperty() {
        return onDelete;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateProperty() {
        return onRegenerate;
    }
}
