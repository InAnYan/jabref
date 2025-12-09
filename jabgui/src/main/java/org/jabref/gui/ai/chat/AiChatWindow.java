package org.jabref.gui.ai.chat;

import java.util.List;

import javafx.collections.ObservableList;
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

    public void setEntries(List<FullBibEntryAiIdentifier> entries) {
        chatView.setEntries(entries);
    }

    public void setChatHistory(ObservableList<ChatHistoryRecordV2> chatHistory) {
        chatView.setChatHistory(chatHistory);
    }
}
