package org.jabref.gui.ai.chat;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.ai.statuspane.SimpleStatusPaneView;
import org.jabref.gui.groups.GroupNodeViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

// [impl->feat~ai.chatting.groups~1]
// [impl->req~ai.chat.groups.ui~1]
public class AiGroupChatView extends StackPane {
    @FXML private AiPrivacyNoticeView privacyNotice;
    @FXML private SimpleStatusPaneView emptyDatabasePathPane;
    @FXML private AiChatView aiChatView;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;

    private AiGroupChatViewModel viewModel;

    public AiGroupChatView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiGroupChatViewModel(preferences.getAiPreferences(), aiService);

        setupBindings();
    }

    private void setupBindings() {
        privacyNotice.managedProperty().bind(privacyNotice.visibleProperty());
        emptyDatabasePathPane.managedProperty().bind(emptyDatabasePathPane.visibleProperty());
        aiChatView.managedProperty().bind(aiChatView.visibleProperty());

        privacyNotice.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiGroupChatViewModel.State.AI_TURNED_OFF));
        emptyDatabasePathPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiGroupChatViewModel.State.NO_DATABASE_PATH));
        aiChatView.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiGroupChatViewModel.State.CHATTING));

        aiChatView.chatHistoryProperty().bind(viewModel.chatHistoryProperty());
        aiChatView.entriesProperty().bind(viewModel.entriesProperty());
    }

    public ObjectProperty<GroupNodeViewModel> groupNodeProperty() {
        return viewModel.groupNodeProperty();
    }

    public ObjectProperty<BibDatabaseContext> databaseContextProperty() {
        return viewModel.databaseContextProperty();
    }
}
