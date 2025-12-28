package org.jabref.gui.ai.chat;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import org.jabref.gui.ai.statuspane.SimpleStatusPaneView;
import org.jabref.logic.ai.AiService;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiEntryChatView extends StackPane {
    @FXML private SimpleStatusPaneView emptyDatabasePathPane;
    @FXML private SimpleStatusPaneView emptyCitationKeyPane;
    @FXML private SimpleStatusPaneView nonUniqueCitationKeyPane;
    @FXML private AiChatView aiChatView;

    @Inject private AiService aiService;

    private AiEntryChatViewModel viewModel;

    public AiEntryChatView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiEntryChatViewModel(aiService);

        emptyDatabasePathPane.managedProperty().bind(emptyDatabasePathPane.visibleProperty());
        emptyCitationKeyPane.managedProperty().bind(emptyCitationKeyPane.visibleProperty());
        nonUniqueCitationKeyPane.managedProperty().bind(nonUniqueCitationKeyPane.visibleProperty());
        aiChatView.managedProperty().bind(aiChatView.visibleProperty());

        emptyDatabasePathPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.NO_DATABASE_PATH));
        emptyCitationKeyPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.NO_CITATION_KEY));
        nonUniqueCitationKeyPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.CITATION_KEY_NOT_UNIQUE));
        aiChatView.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiEntryChatViewModel.State.CHATTING));

        aiChatView.chatHistoryProperty().bind(viewModel.chatHistoryProperty());
        aiChatView.entriesProperty().bind(viewModel.entriesProperty());
    }

    public ObjectProperty<FullBibEntryAiIdentifier> selectedEntryProperty() {
        return viewModel.selectedEntryProperty();
    }
}
