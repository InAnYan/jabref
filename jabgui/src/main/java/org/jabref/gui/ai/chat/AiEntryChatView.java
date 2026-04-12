package org.jabref.gui.ai.chat;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.ai.statuspane.SimpleStatusPaneView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiEntryChatView extends StackPane {
    @FXML private AiPrivacyNoticeView privacyNotice;
    @FXML private SimpleStatusPaneView emptyDatabasePathPane;
    @FXML private SimpleStatusPaneView emptyCitationKeyPane;
    @FXML private SimpleStatusPaneView nonUniqueCitationKeyPane;
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
        privacyNotice.managedProperty().bind(privacyNotice.visibleProperty());
        emptyDatabasePathPane.managedProperty().bind(emptyDatabasePathPane.visibleProperty());
        emptyCitationKeyPane.managedProperty().bind(emptyCitationKeyPane.visibleProperty());
        nonUniqueCitationKeyPane.managedProperty().bind(nonUniqueCitationKeyPane.visibleProperty());
        aiChatView.managedProperty().bind(aiChatView.visibleProperty());

        privacyNotice.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.AI_TURNED_OFF));
        emptyDatabasePathPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.NO_DATABASE_PATH));
        emptyCitationKeyPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.NO_CITATION_KEY));
        nonUniqueCitationKeyPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.CITATION_KEY_NOT_UNIQUE));
        aiChatView.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.CHATTING));

        aiChatView.chatHistoryProperty().bind(viewModel.chatHistoryProperty());
        aiChatView.entriesProperty().bind(viewModel.entriesProperty());
    }

    public ObjectProperty<FullBibEntry> selectedEntryProperty() {
        return viewModel.selectedEntryProperty();
    }
}
