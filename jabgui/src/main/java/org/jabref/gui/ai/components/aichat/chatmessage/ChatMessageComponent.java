package org.jabref.gui.ai.components.aichat.chatmessage;

import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.MarkdownTextFlow;
import org.jabref.logic.ai.util.ErrorMessage;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import org.jabref.logic.ai.framework.messages.ChatMessage;
import org.jabref.logic.ai.framework.messages.LlmMessage;
import org.jabref.logic.ai.framework.messages.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatMessageComponent extends HBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageComponent.class);

    private final ObjectProperty<ChatMessage> chatMessage = new SimpleObjectProperty<>();
    private final ObjectProperty<Consumer<ChatMessageComponent>> onDelete = new SimpleObjectProperty<>();

    @FXML private HBox wrapperHBox;
    @FXML private VBox vBox;
    @FXML private Label sourceLabel;
    @FXML private Pane markdownContentPane;
    @FXML private VBox buttonsVBox;

    private final MarkdownTextFlow markdownTextFlow;

    public ChatMessageComponent() {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        chatMessage.addListener((_, _, newValue) -> {
            if (newValue != null) {
                loadChatMessage();
            }
        });

        markdownTextFlow = new MarkdownTextFlow(markdownContentPane);
        markdownContentPane.getChildren().add(markdownTextFlow);
        markdownContentPane.minHeightProperty().bind(markdownTextFlow.heightProperty());
        markdownContentPane.prefHeightProperty().bind(markdownTextFlow.heightProperty());
    }

    public ChatMessageComponent(ChatMessage chatMessage, Consumer<ChatMessageComponent> onDeleteCallback) {
        this();
        setChatMessage(chatMessage);
        setOnDelete(onDeleteCallback);
    }

    public void setChatMessage(ChatMessage chatMessage) {
        this.chatMessage.set(chatMessage);
    }

    public ChatMessage getChatMessage() {
        return chatMessage.get();
    }

    public void setOnDelete(Consumer<ChatMessageComponent> onDeleteCallback) {
        this.onDelete.set(onDeleteCallback);
    }

    private void loadChatMessage() {
        ChatMessage message = chatMessage.get();

        if (message instanceof UserMessage userMessage) {
            setColor("-jr-ai-message-user", "-jr-ai-message-user-border");
            setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            wrapperHBox.setAlignment(Pos.TOP_RIGHT);
            sourceLabel.setText(Localization.lang("User"));
            markdownTextFlow.setMarkdown(userMessage.getText());
        } else if (message instanceof LlmMessage llmMessage) {
            setColor("-jr-ai-message-ai", "-jr-ai-message-ai-border");
            setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            wrapperHBox.setAlignment(Pos.TOP_LEFT);
            sourceLabel.setText(Localization.lang("AI"));
            markdownTextFlow.setMarkdown(llmMessage.getText());
        } else if (message instanceof ErrorMessage errorMessage) {
            setColor("-jr-ai-message-error", "-jr-ai-message-error-border");
            setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            sourceLabel.setText(Localization.lang("Error"));
            markdownTextFlow.setMarkdown(errorMessage.getText());
        } else {
            setColor("-jr-ai-message-ai", "-jr-ai-message-ai-border");
            setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            wrapperHBox.setAlignment(Pos.TOP_LEFT);
            sourceLabel.setText(Localization.lang("Unknown"));
            markdownTextFlow.setMarkdown(message.getText());
        }
    }

    @FXML
    private void initialize() {
        buttonsVBox.visibleProperty().bind(wrapperHBox.hoverProperty());
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    @FXML
    private void onDeleteClick() {
        if (onDelete.get() != null) {
            onDelete.get().accept(this);
        }
    }

    private void setColor(String fillColor, String borderColor) {
        vBox.setStyle("-fx-background-color: " + fillColor + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: " + borderColor + "; -fx-border-width: 3;");
    }
}
