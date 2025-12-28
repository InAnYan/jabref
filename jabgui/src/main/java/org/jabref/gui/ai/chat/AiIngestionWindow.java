package org.jabref.gui.ai.chat;

import java.util.List;

import javafx.fxml.FXML;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class AiIngestionWindow extends BaseDialog<Void> {
    private final List<GenerateEmbeddingsTask> tasks;

    @FXML private AiIngestionView ingestionView;

    public AiIngestionWindow(
            List<GenerateEmbeddingsTask> tasks
    ) {
        super();
        this.tasks = tasks;

        this.setTitle(Localization.lang("AI ingestion status"));
        this.getDialogPane().getScene().getWindow().setOnCloseRequest(_ -> this.hide());

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        tasks.forEach(ingestionView::addTask);
    }
}
