package org.jabref.gui.ai.components.aichat.chathistory;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.chatmessage.ChatMessageComponent;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.ChatMessage;

public class ChatHistoryComponent extends ScrollPane {
    @FXML private VBox vBox;

    private DialogService dialogService;

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

    public void setDialogService(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    /// @implNote You must call this method only once.
    public void setItems(ObservableList<ChatMessage> items) {
        fill(items);
        items.addListener((ListChangeListener<? super ChatMessage>) obs -> fill(items));
    }

    private void fill(ObservableList<ChatMessage> items) {
        UiTaskExecutor.runInJavaFXThread(() -> {
            vBox.getChildren().clear();
            items.forEach(chatMessage ->
                    vBox.getChildren().add(new ChatMessageComponent(chatMessage, chatMessageComponent -> {
                        if (dialogService != null) {
                            boolean agreed = dialogService.showConfirmationDialogAndWait(
                                    Localization.lang("Delete message"),
                                    Localization.lang("Are you sure you want to delete this message from the chat history?")
                            );
                            if (!agreed) {
                                return;
                            }
                        }
                        int index = vBox.getChildren().indexOf(chatMessageComponent);
                        items.remove(index);
                    })));
        });
    }

    public void scrollDown() {
        this.layout();
        this.setVvalue(this.getVmax());
    }
}
