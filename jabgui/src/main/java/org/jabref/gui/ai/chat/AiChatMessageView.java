package org.jabref.gui.ai.chat;

import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.MarkdownTextFlow;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.messages.ErrorMessage;

import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.UserMessage;

public class AiChatMessageView extends HBox {
    @FXML private VBox vBox;
    @FXML private Label sourceLabel;
    @FXML private StackPane markdownContentPane;
    @FXML private VBox buttonsVBox;

    private MarkdownTextFlow markdownTextFlow;

    private AiChatMessageViewModel viewModel;

    public AiChatMessageView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        this.viewModel = new AiChatMessageViewModel();

        markdownTextFlow = new MarkdownTextFlow(markdownContentPane);
        markdownContentPane.getChildren().add(markdownTextFlow);

        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        buttonsVBox.visibleProperty().bind(this.hoverProperty());

        sourceLabel.textProperty().bind(viewModel.sourceProperty());
        // Other properties of a chat message are bound in the listeners.
    }

    private void setupListeners() {
        // We can't bind the content easily, as the content should be rendered as Markdown.
        viewModel.messageContentProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                markdownTextFlow.setMarkdown(newValue);
            }
        });

        // Also, we need to change the alignment and colors based on the message type.
        viewModel.chatMessageProperty().addListener((_, _, newValue) -> {
            String type = newValue.messageTypeClassName();

            this.getChildren().clear();

            if (Objects.equals(type, UserMessage.class.getName())) {
                this.getChildren().addAll(buttonsVBox, vBox);
                this.setAlignment(Pos.TOP_RIGHT);
                setColor("-jr-ai-message-user", "-jr-ai-message-user-border");
            } else {
                this.getChildren().addAll(vBox, buttonsVBox);
                this.setAlignment(Pos.TOP_LEFT);

                if (Objects.equals(type, ErrorMessage.class.getName())) {
                    setColor("-jr-ai-message-error", "-jr-ai-message-error-border");
                } else {
                    setColor("-jr-ai-message-ai", "-jr-ai-message-ai-border");
                }
            }
        });
    }

    private void setColor(String fillColor, String borderColor) {
        vBox.setStyle("-fx-background-color: " + fillColor + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: " + borderColor + "; -fx-border-width: 1; -fx-padding: 10; -fx-max-width: 600;");
    }

    @FXML
    private void onDeleteClick() {
        viewModel.delete();
    }

    @FXML
    private void onRegenerateClick() {
        viewModel.regenerate();
    }

    public ObjectProperty<ChatHistoryRecordV2> chatMessageProperty() {
        return viewModel.chatMessageProperty();
    }

    public ChatHistoryRecordV2 getChatMessage() {
        return viewModel.chatMessageProperty().get();
    }

    public void setChatMessage(ChatHistoryRecordV2 chatMessage) {
        viewModel.chatMessageProperty().set(chatMessage);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onDeleteProperty() {
        return viewModel.onDeleteProperty();
    }

    public EventHandler<ActionEvent> getOnDelete() {
        return viewModel.onDeleteProperty().get();
    }

    public void setOnDelete(EventHandler<ActionEvent> onDelete) {
        viewModel.onDeleteProperty().set(onDelete);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateProperty() {
        return viewModel.onRegenerateProperty();
    }

    public EventHandler<ActionEvent> getOnRegenerate() {
        return viewModel.onRegenerateProperty().get();
    }

    public void setOnRegenerate(EventHandler<ActionEvent> onRegenerate) {
        viewModel.onRegenerateProperty().set(onRegenerate);
    }

    public ReadOnlyStringProperty messageIdProperty() {
        return viewModel.idProperty();
    }

    public String getMessageIdProperty() {
        return viewModel.idProperty().get();
    }
}
