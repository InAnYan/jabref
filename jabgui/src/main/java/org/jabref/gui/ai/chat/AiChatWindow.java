package org.jabref.gui.ai.chat;

import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;

import org.jabref.gui.util.BaseDialog;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;

import com.airhacks.afterburner.views.ViewLoader;

public class AiChatWindow extends BaseDialog<Void> {
    @FXML private AiChatView chatView;

    public AiChatWindow() {
        super();

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    public ListProperty<ChatHistoryRecordV2> chatHistoryProperty() {
        return chatView.chatHistoryProperty();
    }

    public ListProperty<FullBibEntryAiIdentifier> entriesProperty() {
        return chatView.entriesProperty();
    }
}
