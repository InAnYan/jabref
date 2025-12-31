package org.jabref.gui.ai.chat;

import java.nio.file.Path;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.AnswerEngineKind;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiChatStatusView extends VBox {
    @FXML private TableView<BibEntryAiIdentifier> entriesTable;
    @FXML private TableColumn<BibEntryAiIdentifier, String> libraryColumn;
    @FXML private TableColumn<BibEntryAiIdentifier, String> citationKeyColumn;

    @FXML private TableView<AiChatStatusViewModel.IngestionStatusRow> ingestionTable;
    @FXML private TableColumn<AiChatStatusViewModel.IngestionStatusRow, String> fileColumn;
    @FXML private TableColumn<AiChatStatusViewModel.IngestionStatusRow, AiChatStatusViewModel.FileStatus> statusColumn;
    @FXML private TableColumn<AiChatStatusViewModel.IngestionStatusRow, AiChatStatusViewModel.IngestionStatusRow> actionColumn;

    @FXML private ComboBox<AnswerEngineKind> answerEngineComboBox;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;
    @Inject private DialogService dialogService;

    private AiChatStatusViewModel viewModel;

    public AiChatStatusView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiChatStatusViewModel(
                preferences.getAiPreferences(),
                preferences.getFilePreferences(),
                aiService.getEmbeddingFeature().getCurrentEmbeddingModel(),
                aiService.getIngestionFeature().getEmbeddingsStore()
        );

        setupEntriesTable();
        setupIngestionTable();
        setupParameters();
    }

    private void setupEntriesTable() {
        entriesTable.itemsProperty().bind(viewModel.entriesProperty());

        citationKeyColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(
                        cellData.getValue().entry().getCitationKey().orElse("")
                )
        );
        new ValueTableCellFactory<BibEntryAiIdentifier, String>()
                .withText(text -> text)
                .install(citationKeyColumn);

        libraryColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(
                        cellData.getValue().databaseContext()
                                .getDatabasePath()
                                .map(Path::toString)
                                .orElse("")
                )
        );
        new ValueTableCellFactory<BibEntryAiIdentifier, String>()
                .withText(text -> text)
                .install(libraryColumn);
    }

    private void setupIngestionTable() {
        ingestionTable.setItems(viewModel.getIngestionStatuses());

        fileColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<AiChatStatusViewModel.IngestionStatusRow, String>()
                .withText(text -> text)
                .install(fileColumn);

        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        new ValueTableCellFactory<AiChatStatusViewModel.IngestionStatusRow, AiChatStatusViewModel.FileStatus>()
                .withText(AiChatStatusViewModel.FileStatus::getDisplayName)
                .install(statusColumn);

        actionColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        new ValueTableCellFactory<AiChatStatusViewModel.IngestionStatusRow, AiChatStatusViewModel.IngestionStatusRow>()
                .withGraphic(row -> {
                    if (row.getStatus() == AiChatStatusViewModel.FileStatus.ERROR_WHILE_PROCESSING) {
                        return constructErrorButton(row);
                    }
                    return null;
                })
                .install(actionColumn);
    }

    private Button constructErrorButton(AiChatStatusViewModel.IngestionStatusRow row) {
        Button errorButton = new Button(Localization.lang("Show Error"));
        errorButton.getStyleClass().add("text-button");
        errorButton.setOnAction(event ->
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Ingestion Error"),
                        row.getError()
                )
        );
        return errorButton;
    }

    private void setupParameters() {
        new ViewModelListCellFactory<AnswerEngineKind>()
                .withText(AnswerEngineKind::getDisplayName)
                .install(answerEngineComboBox);
        answerEngineComboBox.setItems(viewModel.answerEngineKindsProperty());
        answerEngineComboBox.valueProperty().bindBidirectional(viewModel.selectedAnswerEngineKindProperty());
    }

    public ListProperty<AnswerEngineKind> answerEngineKindsProperty() {
        return viewModel.answerEngineKindsProperty();
    }

    public ObjectProperty<AnswerEngineKind> selectedAnswerEngineKindProperty() {
        return viewModel.selectedAnswerEngineKindProperty();
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return viewModel.answerEngineProperty();
    }

    public ListProperty<BibEntryAiIdentifier> entriesProperty() {
        return viewModel.entriesProperty();
    }

    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return viewModel.generateEmbeddingsTasksProperty();
    }
}
