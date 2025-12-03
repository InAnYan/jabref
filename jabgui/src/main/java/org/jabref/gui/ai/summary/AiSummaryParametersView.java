package org.jabref.gui.ai.summary;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.ai.summarization.SummarizatorKind;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiSummaryParametersView extends VBox {
    private AiSummaryParametersViewModel viewModel;

    @FXML
    private ComboBox<SummarizatorKind> summarizatorCombo;

    @Inject
    private AiService aiService;

    public AiSummaryParametersView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        this.viewModel = new AiSummaryParametersViewModel(aiService);

        new ViewModelListCellFactory<SummarizatorKind>()
                .withText(SummarizatorKind::getDisplayName)
                .install(summarizatorCombo);
        summarizatorCombo.setItems(viewModel.summarizatorKindsProperty());

        summarizatorCombo.valueProperty().bindBidirectional(viewModel.summarizatorKindProperty());
    }

    public Summarizator constructSummarizator() {
        return viewModel.constructSummarizator();
    }
}
