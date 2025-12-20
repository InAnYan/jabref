package org.jabref.gui.ai.chat;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.MarkdownTextFlow;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.messages.ErrorMessage;

import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

public class AiChatMessageView extends HBox {
    @FXML private VBox vBox;
    @FXML private Label sourceLabel;
    @FXML private Pane markdownContentPane;
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
        markdownContentPane.minHeightProperty().bind(markdownTextFlow.heightProperty());
        markdownContentPane.prefHeightProperty().bind(markdownTextFlow.heightProperty());

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

        if (Objects.equals(type, UserMessage.class.getName())) {
            setColor("-jr-ai-message-user", "-jr-ai-message-user-border");
            setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            this.setAlignment(Pos.TOP_RIGHT);
        } else if (Objects.equals(type, AiMessage.class.getName())) {
            setColor("-jr-ai-message-ai", "-jr-ai-message-ai-border");
            setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            this.setAlignment(Pos.TOP_LEFT);
        } else if (Objects.equals(type, ErrorMessage.class.getName())) {
            setColor("-jr-ai-message-error", "-jr-ai-message-error-border");
            setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        } else {
            // Fall back to AI.
            setColor("-jr-ai-message-ai", "-jr-ai-message-ai-border");
            setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            this.setAlignment(Pos.TOP_LEFT);
        }
    }

    private void setColor(String fillColor, String borderColor) {
        vBox.setStyle("-fx-background-color: " + fillColor + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: " + borderColor + "; -fx-border-width: 3;");
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
