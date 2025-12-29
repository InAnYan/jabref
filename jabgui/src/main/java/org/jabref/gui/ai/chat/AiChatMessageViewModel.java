package org.jabref.gui.ai.chat;

import java.time.Instant;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.ai.chatting.util.ChatHistoryRecordUtils;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;

public class AiChatMessageViewModel extends AbstractViewModel {
    private final ObjectProperty<ChatHistoryRecordV2> chatMessage = new SimpleObjectProperty<>();

    private final StringProperty id = new SimpleStringProperty("");
    private final StringProperty source = new SimpleStringProperty("");
    private final StringProperty messageContent = new SimpleStringProperty("");
    private final ObjectProperty<Instant> timestamp = new SimpleObjectProperty<>(Instant.now());

    private final ObjectProperty<EventHandler<ActionEvent>> onDelete = new SimpleObjectProperty<>(_ -> {
    });
    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerate = new SimpleObjectProperty<>(_ -> {
    });

    public AiChatMessageViewModel() {
        this.chatMessage.addListener((_, _, value) -> {
            id.set(value.id());
            source.set(ChatHistoryRecordUtils.getMessageAuthorDisplayName(value.messageTypeClassName()));
            messageContent.set(value.content());
            timestamp.set(value.createdAt());
        });
    }

    public ObjectProperty<ChatHistoryRecordV2> chatMessageProperty() {
        return chatMessage;
    }

    public StringProperty idProperty() {
        return id;
    }

    public StringProperty sourceProperty() {
        return source;
    }

    public StringProperty messageContentProperty() {
        return messageContent;
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
