package org.jabref.gui.ai.chat;

import java.time.Instant;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.logic.ai.chatting.util.ChatHistoryRecordUtils;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;

public class AiChatMessageViewModel {
    private final StringProperty id = new SimpleStringProperty("");
    private final StringProperty source = new SimpleStringProperty("");
    private final StringProperty message = new SimpleStringProperty("");
    private final ObjectProperty<Instant> timestamp = new SimpleObjectProperty<>(Instant.now());

    private final ObjectProperty<EventHandler<ActionEvent>> onDelete = new SimpleObjectProperty<>(_ -> {});
    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerate = new SimpleObjectProperty<>(_ -> {});

    public void set(ChatHistoryRecordV2 chatMessage) {
        id.set(chatMessage.id());
        source.set(ChatHistoryRecordUtils.getMessageAuthorDisplayName(chatMessage.messageTypeClassName()));
        message.set(chatMessage.content());
        timestamp.set(chatMessage.createdAt());
    }

    public StringProperty idProperty() {
        return id;
    }

    public StringProperty sourceProperty() {
        return source;
    }

    public StringProperty messageProperty() {
        return message;
    }

    public ObjectProperty<Instant> timestampProperty() {
        return timestamp;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onDeleteProperty() {
        return onDelete;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateProperty() {
        return onRegenerate;
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
}
