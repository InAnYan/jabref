package org.jabref.gui.ai.summary;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.ai.summarization.SummarizatorKind;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiSummaryParametersView extends VBox {
    @FXML private ComboBox<SummarizatorKind> summarizatorCombo;

    @Inject private GuiPreferences preferences;

    private AiSummaryParametersViewModel viewModel;

    public AiSummaryParametersView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        this.viewModel = new AiSummaryParametersViewModel(
                preferences.getAiPreferences()
        );

        setupBindings();
    }

    private void setupBindings() {
        new ViewModelListCellFactory<SummarizatorKind>()
                .withText(SummarizatorKind::getDisplayName)
                .install(summarizatorCombo);

        summarizatorCombo.itemsProperty().bind(viewModel.summarizatorKindsProperty());
        summarizatorCombo.valueProperty().bindBidirectional(viewModel.summarizatorKindProperty());
    }

    public Summarizator constructSummarizator() {
        return viewModel.constructSummarizator();
    }
}
