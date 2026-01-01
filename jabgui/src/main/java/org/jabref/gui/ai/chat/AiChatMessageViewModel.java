package org.jabref.gui.ai.chat;

import java.time.Instant;

import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.PropertiesHelper;
import org.jabref.logic.ai.chatting.util.ChatHistoryRecordUtils;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.ErrorMessage;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

public class AiChatMessageViewModel extends AbstractViewModel {
    private final ObjectProperty<ChatHistoryRecordV2> chatMessage = new SimpleObjectProperty<>();

    private final StringProperty id = new SimpleStringProperty("");
    private final StringProperty source = new SimpleStringProperty("");
    private final StringProperty messageContent = new SimpleStringProperty("");
    private final ObjectProperty<Instant> timestamp = new SimpleObjectProperty<>(Instant.now());

    private final BooleanProperty showDelete = new SimpleBooleanProperty(true);
    private final BooleanProperty showRegenerate = new SimpleBooleanProperty(false);
    private final BooleanProperty showEdit = new SimpleBooleanProperty(false);

    private final ObjectProperty<EventHandler<ActionEvent>> onDelete = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerate = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onEdit = new SimpleObjectProperty<>();

    public AiChatMessageViewModel() {
        setupBindings();
    }

    private void setupBindings() {
        id.bind(chatMessage.map(ChatHistoryRecordV2::id));
        source.bind(chatMessage
                .map(ChatHistoryRecordV2::messageTypeClassName)
                .map(ChatHistoryRecordUtils::getMessageAuthorDisplayName));
        messageContent.bind(chatMessage.map(ChatHistoryRecordV2::content));
        timestamp.bind(chatMessage.map(ChatHistoryRecordV2::createdAt));

        StringExpression messageType = StringExpression.stringExpression(chatMessage.map(ChatHistoryRecordV2::messageTypeClassName));
        showEdit.bind(messageType.isEqualTo(UserMessage.class.getName()));
        showRegenerate.bind(messageType.isEqualTo(AiMessage.class.getName()).or(messageType.isEqualTo(ErrorMessage.class.getName())));
    }

    public void delete() {
        PropertiesHelper.handle(onDelete);
    }

    public void regenerate() {
        PropertiesHelper.handle(onRegenerate);
    }

    public void edit() {
        PropertiesHelper.handle(onEdit);
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

    public ReadOnlyBooleanProperty showDeleteProperty() {
        return showDelete;
    }

    public ReadOnlyBooleanProperty showRegenerateProperty() {
        return showRegenerate;
    }

    public ReadOnlyBooleanProperty showEditProperty() {
        return showEdit;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onDeleteProperty() {
        return onDelete;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateProperty() {
        return onRegenerate;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onEditProperty() {
        return onEdit;
    }
}
