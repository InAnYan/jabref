package org.jabref.gui.ai.chat;

import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.LinkedFile;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiIngestionView extends VBox {
    private AiIngestionViewModel viewModel;

    @FXML private GridPane grid;

    @Inject private DialogService dialogService;

    public AiIngestionView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiIngestionViewModel();

        viewModel.ingestionStateMapProperty().addListener((InvalidationListener) _ -> updateTasks());
    }

    private void updateTasks() {
        // It's easier to just clear the grid and repopulate it. But inefficient.

        grid.getChildren().clear();

        for (Map.Entry<LinkedFile, AiIngestionViewModel.IngestionState> entry : viewModel.ingestionStateMapProperty().entrySet()) {
            Label name = new Label(entry.getKey().getLink());
            grid.getChildren().add(name);

            switch (entry.getValue().status()) {
                case PENDING -> {
                    Label status = new Label(Localization.lang("Pending"));
                    grid.getChildren().add(status);
                }
                case PROCESSING -> {
                    Label status = new Label(Localization.lang("Processing"));
                    grid.getChildren().add(status);
                }
                case ERROR_WHILE_PROCESSING -> {
                    Button button = new Button(Localization.lang("Problem"));
                    button.setOnAction(_ -> {
                        dialogService.showErrorDialogAndWait(Localization.lang("Problem while processing"), entry.getValue().error());
                    });
                    grid.getChildren().add(button);
                }
                case INGESTED -> {
                    Label status = new Label(Localization.lang("Ingested"));
                    grid.getChildren().add(status);
                }
                case CANCELLED -> {
                    Label status = new Label(Localization.lang("Cancelled"));
                    grid.getChildren().add(status);
                }
            }
        }
    }

    public void addTask(GenerateEmbeddingsTask task) {
        viewModel.addTask(task);
    }
}
