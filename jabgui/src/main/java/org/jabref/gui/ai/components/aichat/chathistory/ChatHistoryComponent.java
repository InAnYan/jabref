package org.jabref.gui.ai.components.aichat.chathistory;

import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.ai.components.aichat.chatmessage.ChatMessageComponent;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.chatting.util.ChatHistory;

import com.airhacks.afterburner.views.ViewLoader;

public class ChatHistoryComponent extends ScrollPane {
    @FXML private VBox vBox;

    public ChatHistoryComponent() {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.needsLayoutProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                scrollDown();
            }
        });
    }

    public void updateMessages(ChatHistory chatHistory) {
        UiTaskExecutor.runInJavaFXThread(() -> {
            vBox.getChildren().clear();
            // TODO: Inefficient.
            chatHistory
                    .getAllMessages()
                    .forEach(chatMessage ->
                            vBox.getChildren().add(new ChatMessageComponent(
                                    chatMessage,
                                    chatMessageComponent -> {
                                        // TODO: Mix of logic.
                                        chatHistory.deleteMessage(chatMessageComponent.getChatMessage().id());
                                        updateMessages(chatHistory);
                                    })
                            )
                    );
        });
    }

    public void scrollDown() {
        this.layout();
        this.setVvalue(this.getVmax());
    }

    public Optional<ChatMessageComponent> getMessage(int index) {
        return Optional.ofNullable(vBox.getChildren().get(index)).map(node -> (ChatMessageComponent) node);
    }
}
