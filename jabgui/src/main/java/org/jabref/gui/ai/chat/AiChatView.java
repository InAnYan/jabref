package org.jabref.gui.ai.chat;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ListScrollPane;
import org.jabref.logic.ai.AiService;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiChatView extends VBox {
    private AiChatViewModel viewModel;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;
    @Inject private DialogService dialogService;

    @FXML private ListScrollPane<ChatHistoryRecordV2> chatHistoryScrollPane;

    @FXML private Button infoButton;
    @FXML private Button userMessage;

    public AiChatView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiChatViewModel(
                preferences,
                aiService,
                dialogService
        );
    }

    public void setEntries(List<FullBibEntryAiIdentifier> entries) {
        viewModel.setEntries(entries);
    }

    public void setChatHistory(ObservableList<ChatHistoryRecordV2> chatHistory) {
        viewModel.setChatHistory(chatHistory);
    }
}
