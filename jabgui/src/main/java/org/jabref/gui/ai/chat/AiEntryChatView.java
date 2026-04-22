package org.jabref.gui.ai.chat;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

// [impl->feat~ai.chatting.entries~1]
public class AiEntryChatView extends StackPane {
    @FXML private AiPrivacyNoticeView privacyNotice;
    @FXML private AiChatView aiChatView;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;

    private AiEntryChatViewModel viewModel;

    public AiEntryChatView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiEntryChatViewModel(
                preferences.getAiPreferences(),
                aiService.getChatHistoryCache()
        );

        setupBindings();
    }

    private void setupBindings() {
        // [pp->feat~ai.chatting.entries~1]
        privacyNotice.managedProperty().bind(privacyNotice.visibleProperty());
        aiChatView.managedProperty().bind(aiChatView.visibleProperty());

        privacyNotice.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.AI_TURNED_OFF));
        aiChatView.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.CHATTING));

        aiChatView.chatHistoryProperty().bind(viewModel.chatHistoryProperty());
        aiChatView.entriesProperty().bind(viewModel.entriesProperty());
    }

    public ObjectProperty<FullBibEntry> selectedEntryProperty() {
        return viewModel.selectedEntryProperty();
    }
}
