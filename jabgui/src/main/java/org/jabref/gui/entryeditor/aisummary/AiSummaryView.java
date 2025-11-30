package org.jabref.gui.entryeditor.aisummary;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.ai.statuspane.ErrorStatusPaneView;
import org.jabref.gui.ai.statuspane.LoadingStatusPaneView;
import org.jabref.gui.ai.statuspane.SimpleStatusPaneView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiSummaryView extends StackPane {
    @FXML private AiPrivacyNoticeView privacyNotice;

    @FXML private BorderPane pendingPane;
    @FXML private ComboBox<SummarizatorKind> summarizatorCombo;
    @FXML private Button generateButton;
    @FXML private Label pendingHint;

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
                aiService
        );

        privacyNotice.visibleProperty().bind(viewModel.showAiPrivacyPolicyGuardProperty());
        privacyNotice.managedProperty().bind(viewModel.showAiPrivacyPolicyGuardProperty());

        viewModel.processingAiProviderProperty().addListener(_ -> updateHints());
        viewModel.processingLlmNameProperty().addListener(_ -> updateHints());
        viewModel.selectedSummarizatorKindProperty().addListener(_ -> updateHints());

        summarizatorCombo.itemsProperty().bind(viewModel.summarizatorKindsProperty());
        summarizatorCombo.valueProperty().bindBidirectional(viewModel.selectedSummarizatorKindProperty());

        generateButton.setOnAction(_ -> viewModel.generate());

        summaryShowing.bibEntrySummaryProperty().bind(viewModel.summaryProperty());

        viewModel.stateProperty().addListener((_, _, newState) -> updateStateView(newState));
        updateStateView(viewModel.stateProperty().get());
    }

    private void updateStateView(AiSummaryViewModel.State state) {
        pendingPane.setVisible(false);
        pendingPane.setManaged(false);
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
            case PENDING -> {
                boolean show = !viewModel.showAiPrivacyPolicyGuardProperty().get();
                pendingPane.setVisible(show);
                pendingPane.setManaged(show);
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
        viewModel.bindEntry(new FullBibEntryAiIdentifier(bibDatabaseContext, entry));
    }

    private void updateHints() {
        pendingHint.setText(Localization.lang(
                "Your entry will be processed by %0 %1",
                viewModel.processingAiProviderProperty().get().getDisplayName(),
                viewModel.processingLlmNameProperty().get())
        );

        processingPane.setDescription(Localization.lang(
                "Your entry is being summarized by %0 %1 using algorithm %3",
                viewModel.processingAiProviderProperty().get().getDisplayName(),
                viewModel.processingLlmNameProperty().get(),
                viewModel.selectedSummarizatorKindProperty().get().getDisplayName()
        ));
    }

    @FXML
    private void regenerate() {
        viewModel.regenerate();
    }

    @FXML
    private void cancel() {
        viewModel.cancel();
    }
}
