package org.jabref.gui.ai.chat;

import java.nio.file.Path;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.AnswerEngineKind;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiChatStatusView extends VBox {

    @FXML private TableView<FullBibEntryAiIdentifier> entriesTable;
    @FXML private TableColumn<FullBibEntryAiIdentifier, String> libraryColumn;
    @FXML private TableColumn<FullBibEntryAiIdentifier, String> citationKeyColumn;

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
        viewModel = new AiChatStatusViewModel(preferences, aiService);

        setupEntriesTable();
        setupIngestionTable();
        setupParameters();
    }

    private void setupEntriesTable() {
        entriesTable.itemsProperty().bind(viewModel.entriesProperty());

        citationKeyColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().entry().getCitationKey().orElse("")
                )
        );

        libraryColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().databaseContext()
                                .getDatabasePath()
                                .map(Path::toString)
                                .orElse("")
                )
        );
    }

    private void setupIngestionTable() {
        ingestionTable.setItems(viewModel.getIngestionStatuses());

        fileColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        actionColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));

        statusColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(AiChatStatusViewModel.FileStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(switch (status) {
                        case PENDING -> Localization.lang("Pending");
                        case PROCESSING -> Localization.lang("Processing");
                        case ERROR_WHILE_PROCESSING -> Localization.lang("Error");
                        case INGESTED -> Localization.lang("Ingested");
                        case CANCELLED -> Localization.lang("Cancelled");
                    });
                }
            }
        });

        actionColumn.setCellFactory(_ -> new TableCell<>() {
            final Button errorButton = new Button(Localization.lang("Show Error"));

            {
                errorButton.getStyleClass().add("text-button");
            }

            @Override
            protected void updateItem(AiChatStatusViewModel.IngestionStatusRow row, boolean empty) {
                super.updateItem(row, empty);

                if (empty || row == null || row.getStatus() != AiChatStatusViewModel.FileStatus.ERROR_WHILE_PROCESSING) {
                    setGraphic(null);
                } else {
                    errorButton.setOnAction(_ ->
                            dialogService.showErrorDialogAndWait(
                                    Localization.lang("Ingestion Error"),
                                    row.getError()
                            )
                    );
                    setGraphic(errorButton);
                }
            }
        });
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

    public ListProperty<FullBibEntryAiIdentifier> entriesProperty() {
        return viewModel.entriesProperty();
    }

    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return viewModel.generateEmbeddingsTasksProperty();
    }
}
