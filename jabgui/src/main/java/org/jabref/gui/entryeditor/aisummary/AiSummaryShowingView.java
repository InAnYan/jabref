package org.jabref.gui.entryeditor.aisummary;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import com.airhacks.afterburner.views.ViewLoader;

public class AiSummaryShowingView extends VBox {
    @FXML private CheckBox markdownCheckbox;
    @FXML private Text summaryInfoText;

    private AiSummaryShowingViewModel viewModel;

    public AiSummaryShowingView() {
        ViewLoader.view(this)
                .load();
    }
}
