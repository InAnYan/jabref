package org.jabref.gui.entryeditor.aisummary;

import javafx.beans.property.ObjectProperty;
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

        ObjectProperty<AiSummaryViewModel.State> state = viewModel.stateProperty();

        privacyNotice.visibleProperty().bind(viewModel.showAiPrivacyPolicyGuardProperty());
        privacyNotice.managedProperty().bind(viewModel.showAiPrivacyPolicyGuardProperty());

        pendingPane.visibleProperty().bind(state.isEqualTo(AiSummaryViewModel.State.PENDING).and(viewModel.showAiPrivacyPolicyGuardProperty().not()));
        pendingPane.managedProperty().bind(pendingPane.visibleProperty());
        viewModel.processingAiProviderProperty().addListener(_ -> {
            updateHints();
        });
        viewModel.processingLlmNameProperty().addListener(_ -> {
            updateHints();
        });

        processingPane.visibleProperty().bind(state.isEqualTo(AiSummaryViewModel.State.PROCESSING));
        processingPane.managedProperty().bind(processingPane.visibleProperty());
        viewModel.selectedSummarizatorKindProperty().addListener(_ -> updateHints());

        errorPane.visibleProperty().bind(state.isEqualTo(AiSummaryViewModel.State.ERROR_WHILE_GENERATING));
        errorPane.managedProperty().bind(errorPane.visibleProperty());

        noDatabasePathPane.visibleProperty().bind(state.isEqualTo(AiSummaryViewModel.State.NO_DATABASE_PATH));
        noDatabasePathPane.managedProperty().bind(noDatabasePathPane.visibleProperty());

        noCitationKeyPane.visibleProperty().bind(state.isEqualTo(AiSummaryViewModel.State.NO_CITATION_KEY));
        noCitationKeyPane.managedProperty().bind(noCitationKeyPane.visibleProperty());

        wrongCitationKeyPane.visibleProperty().bind(state.isEqualTo(AiSummaryViewModel.State.WRONG_CITATION_KEY));
        wrongCitationKeyPane.managedProperty().bind(wrongCitationKeyPane.visibleProperty());

        noFilesPane.visibleProperty().bind(state.isEqualTo(AiSummaryViewModel.State.NO_FILES));
        noFilesPane.managedProperty().bind(noFilesPane.visibleProperty());

        noSupportedFileTypesPane.visibleProperty().bind(state.isEqualTo(AiSummaryViewModel.State.NO_SUPPORTED_FILE_TYPES));
        noSupportedFileTypesPane.managedProperty().bind(noSupportedFileTypesPane.visibleProperty());

        summarizatorCombo.itemsProperty().bind(viewModel.summarizatorKindsProperty());
        summarizatorCombo.valueProperty().bindBidirectional(viewModel.selectedSummarizatorKindProperty());

        generateButton.setOnAction(_ -> viewModel.generate());
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
