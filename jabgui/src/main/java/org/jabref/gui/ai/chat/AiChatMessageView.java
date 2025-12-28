package org.jabref.gui.ai.chat;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

        buttonsVBox.visibleProperty().bind(this.hoverProperty());

        HBox.setHgrow(this, Priority.ALWAYS);

        sourceLabel.textProperty().bind(viewModel.sourceProperty());
        viewModel.messageProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                markdownTextFlow.setMarkdown(newValue);
            }
        });
    }

    public void setChatMessage(ChatHistoryRecordV2 chatMessage) {
        viewModel.set(chatMessage);

        String type = chatMessage.messageTypeClassName();

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
    }

    private void setColor(String fillColor, String borderColor) {
        vBox.setStyle("-fx-background-color: " + fillColor + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: " + borderColor + "; -fx-border-width: 1; -fx-padding: 10; -fx-max-width: 600;");
    }

    public AiChatMessageViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    private void onDeleteClick() {
        viewModel.delete();
    }

    @FXML
    private void onRegenerateClick() {
        viewModel.regenerate();
    }
}
