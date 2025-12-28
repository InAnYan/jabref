package org.jabref.gui.ai.summary;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.ai.statuspane.ErrorStatusPaneView;
import org.jabref.gui.ai.statuspane.LoadingStatusPaneView;
import org.jabref.gui.ai.statuspane.SimpleStatusPaneView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiSummaryView extends StackPane {
    @FXML private AiPrivacyNoticeView privacyNotice;

    @FXML private LoadingStatusPaneView processingPane;
    @FXML private ErrorStatusPaneView errorPane;

    @FXML private SimpleStatusPaneView noDatabasePathPane;
    @FXML private SimpleStatusPaneView noCitationKeyPane;
    @FXML private SimpleStatusPaneView wrongCitationKeyPane;
    @FXML private SimpleStatusPaneView noFilesPane;
    @FXML private SimpleStatusPaneView noSupportedFileTypesPane;

    @FXML private AiSummaryShowingView summaryShowing;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;
    @Inject private DialogService dialogService;

    private AiSummaryViewModel viewModel;

    public AiSummaryView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiSummaryViewModel(
                preferences,
                aiService,
                dialogService
        );

        errorPane.exceptionProperty().bind(viewModel.errorProperty());
        summaryShowing.summaryProperty().bind(viewModel.summaryProperty());

        viewModel.summarizatorProperty().addListener(_ -> updateHints());
        viewModel.chatModelProperty().addListener(_ -> updateHints());
        updateHints();

        viewModel.stateProperty().addListener(_ -> updateStateView());
        updateStateView();
    }

    private void updateStateView() {
        AiSummaryViewModel.State state = viewModel.stateProperty().get();

        privacyNotice.setVisible(false);
        privacyNotice.setManaged(false);
        processingPane.setVisible(false);
        processingPane.setManaged(false);
        errorPane.setVisible(false);
        errorPane.setManaged(false);
        noDatabasePathPane.setVisible(false);
        noDatabasePathPane.setManaged(false);
        noCitationKeyPane.setVisible(false);
        noCitationKeyPane.setManaged(false);
        wrongCitationKeyPane.setVisible(false);
        wrongCitationKeyPane.setManaged(false);
        noFilesPane.setVisible(false);
        noFilesPane.setManaged(false);
        noSupportedFileTypesPane.setVisible(false);
        noSupportedFileTypesPane.setManaged(false);
        summaryShowing.setVisible(false);
        summaryShowing.setManaged(false);

        switch (state) {
            case AI_TURNED_OFF -> {
                privacyNotice.setVisible(true);
                privacyNotice.setManaged(true);
            }
            case PROCESSING -> {
                processingPane.setVisible(true);
                processingPane.setManaged(true);
            }
            case ERROR_WHILE_GENERATING -> {
                errorPane.setVisible(true);
                errorPane.setManaged(true);
            }
            case NO_DATABASE_PATH -> {
                noDatabasePathPane.setVisible(true);
                noDatabasePathPane.setManaged(true);
            }
            case NO_CITATION_KEY -> {
                noCitationKeyPane.setVisible(true);
                noCitationKeyPane.setManaged(true);
            }
            case WRONG_CITATION_KEY -> {
                wrongCitationKeyPane.setVisible(true);
                wrongCitationKeyPane.setManaged(true);
            }
            case NO_FILES -> {
                noFilesPane.setVisible(true);
                noFilesPane.setManaged(true);
            }
            case NO_SUPPORTED_FILE_TYPES -> {
                noSupportedFileTypesPane.setVisible(true);
                noSupportedFileTypesPane.setManaged(true);
            }
            case DONE -> {
                summaryShowing.setVisible(true);
                summaryShowing.setManaged(true);
            }
        }
    }

    public void bind(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        viewModel.entryProperty().set(new FullBibEntryAiIdentifier(bibDatabaseContext, entry));
    }

    private void updateHints() {
        if (viewModel.chatModelProperty().get() == null || viewModel.summarizatorProperty().get() == null) {
            return;
        }

        processingPane.setDescription(Localization.lang(
                "Your entry is being summarized by %0 %1 using algorithm %2",
                viewModel.chatModelProperty().get().getAiProvider().getDisplayName(),
                viewModel.chatModelProperty().get().getName(),
                viewModel.summarizatorProperty().get().getKind().getDisplayName()
        ));
    }

    @FXML
    private void regenerate() {
        viewModel.regenerate();
    }

    @FXML
    private void regenerateCustom() {
        viewModel.regenerateCustom();
    }

    @FXML
    private void cancel() {
        viewModel.cancel();
    }
}
