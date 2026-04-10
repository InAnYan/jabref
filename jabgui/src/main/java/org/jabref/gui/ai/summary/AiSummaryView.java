package org.jabref.gui.ai.summary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.ai.statuspane.ErrorStatusPaneView;
import org.jabref.gui.ai.statuspane.LoadingStatusPaneView;
import org.jabref.gui.ai.statuspane.SimpleStatusPaneView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.summarization.AiSummaryJsonExporter;
import org.jabref.logic.ai.summarization.AiSummaryMarkdownExporter;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiSummaryView extends StackPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiSummaryView.class);

    @FXML private AiPrivacyNoticeView privacyNotice;

    @FXML private LoadingStatusPaneView processingPane;
    @FXML private ErrorStatusPaneView errorPane;

    @FXML private SimpleStatusPaneView noDatabasePathPane;
    @FXML private SimpleStatusPaneView noFilesPane;
    @FXML private SimpleStatusPaneView noSupportedFileTypesPane;

    @FXML private AiSummaryShowingView summaryShowing;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;
    @Inject private DialogService dialogService;
    @Inject private BibEntryTypesManager entryTypesManager;

    private AiSummaryViewModel viewModel;

    public AiSummaryView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    private static String generateDescription(Summarizator summarizator, ChatModel chatModel) {
        if (summarizator == null || chatModel == null) {
            return "";
        }

        return Localization.lang(
                "Your entry is being summarized by %0 %1 using algorithm %2",
                chatModel.getAiProvider().getDisplayName(),
                chatModel.getName(),
                summarizator.getKind().getDisplayName()
        );
    }

    @FXML
    private void initialize() {
        viewModel = new AiSummaryViewModel(
                preferences.getAiPreferences(),
                preferences.getFilePreferences(),
                aiService.getSummariesRepository(),
                aiService.getSummaryCache(),
                aiService.getSummarizationTaskAggregator(),
                dialogService
        );

        setupBindings();
    }

    private void setupBindings() {
        errorPane.exceptionProperty().bind(viewModel.errorProperty());
        summaryShowing.summaryProperty().bind(viewModel.summaryProperty());

        processingPane.descriptionProperty().bind(BindingsHelper.map(
                viewModel.summarizatorProperty(), viewModel.chatModelProperty(),
                AiSummaryView::generateDescription
        ));

        privacyNotice.managedProperty().bind(privacyNotice.visibleProperty());
        processingPane.managedProperty().bind(processingPane.visibleProperty());
        errorPane.managedProperty().bind(errorPane.visibleProperty());
        noDatabasePathPane.managedProperty().bind(noDatabasePathPane.visibleProperty());
        noFilesPane.managedProperty().bind(noFilesPane.visibleProperty());
        noSupportedFileTypesPane.managedProperty().bind(noSupportedFileTypesPane.visibleProperty());
        summaryShowing.managedProperty().bind(summaryShowing.visibleProperty());

        privacyNotice.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.AI_TURNED_OFF));
        processingPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.PROCESSING));
        errorPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.ERROR_WHILE_GENERATING));
        noDatabasePathPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.NO_DATABASE_PATH));
        noFilesPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.NO_FILES));
        noSupportedFileTypesPane.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.NO_SUPPORTED_FILE_TYPES));
        summaryShowing.visibleProperty().bind(viewModel.stateProperty().isEqualTo(AiSummaryViewModel.State.DONE));
    }

    public ObjectProperty<FullBibEntry> entryProperty() {
        return viewModel.entryProperty();
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

    @FXML
    private void exportMarkdown() {
        AiSummary summary = viewModel.summaryProperty().get();
        FullBibEntry fullEntry = viewModel.getEntry();

        if (summary == null || fullEntry == null) {
            dialogService.notify(Localization.lang("No summary available to export"));
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.MARKDOWN)
                .withDefaultExtension(StandardFileType.MARKDOWN)
                .withInitialDirectory(Path.of(System.getProperty("user.home")))
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(path -> {
                         try {
                             AiSummaryMarkdownExporter exporter = new AiSummaryMarkdownExporter(entryTypesManager, preferences.getFieldPreferences());
                             String content = exporter.export(fullEntry.entry(), fullEntry.databaseContext().getMode(), summary);
                             Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             dialogService.notify(Localization.lang("Export operation finished successfully."));
                         } catch (IOException e) {
                             LOGGER.error("Problem occurred while writing the export file", e);
                             dialogService.showErrorDialogAndWait(Localization.lang("Problem occurred while writing the export file"), e);
                         }
                     });
    }

    @FXML
    private void exportJson() {
        AiSummary summary = viewModel.summaryProperty().get();
        FullBibEntry fullEntry = viewModel.getEntry();

        if (summary == null || fullEntry == null) {
            dialogService.notify(Localization.lang("No summary available to export"));
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.JSON)
                .withDefaultExtension(StandardFileType.JSON)
                .withInitialDirectory(Path.of(System.getProperty("user.home")))
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(path -> {
                         try {
                             AiSummaryJsonExporter exporter = new AiSummaryJsonExporter(entryTypesManager, preferences.getFieldPreferences());
                             String content = exporter.export(fullEntry.entry(), fullEntry.databaseContext().getMode(), summary);
                             Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             dialogService.notify(Localization.lang("Export operation finished successfully."));
                         } catch (IOException e) {
                             LOGGER.error("Problem occurred while writing the export file", e);
                             dialogService.showErrorDialogAndWait(Localization.lang("Problem occurred while writing the export file"), e);
                         }
                     });
    }
}
