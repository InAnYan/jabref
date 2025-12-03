package org.jabref.gui.ai.summary;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.summarization.logic.SummarizatorFactory;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.templates.AiTemplatesFactory;
import org.jabref.model.ai.summarization.SummarizatorKind;

public class AiSummaryParametersViewModel {
    private final ListProperty<SummarizatorKind> summarizatorKinds = new SimpleListProperty<>(
            FXCollections.observableArrayList(SummarizatorKind.values())
    );
    private final ObjectProperty<SummarizatorKind> summarizatorKind = new SimpleObjectProperty<>();

    // In the future: various parameters for summarizators.

    private final AiTemplatesFactory aiTemplatesFactory;

    public AiSummaryParametersViewModel(GuiPreferences preferences, AiService aiService) {
        this.aiTemplatesFactory = aiService.getCurrentAiTemplates();

        this.summarizatorKind.set(preferences.getAiPreferences().getSummarizatorKind());
    }

    public ListProperty<SummarizatorKind> summarizatorKindsProperty() {
        return summarizatorKinds;
    }

    public ObjectProperty<SummarizatorKind> summarizatorKindProperty() {
        return summarizatorKind;
    }

    public Summarizator constructSummarizator() {
        return SummarizatorFactory.createSummarizator(aiTemplatesFactory, summarizatorKind.get());
    }
}
