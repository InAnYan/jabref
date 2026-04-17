package org.jabref.gui.ai.summary;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.SummarizatorFactory;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.ai.summarization.SummarizatorKind;

public class AiSummaryParametersViewModel extends AbstractViewModel {
    private final ListProperty<SummarizatorKind> summarizatorKinds = new SimpleListProperty<>(
            FXCollections.observableArrayList(SummarizatorKind.values())
    );
    private final ObjectProperty<SummarizatorKind> summarizatorKind = new SimpleObjectProperty<>();

    // Reactive binding that rebuilds the Summarizator whenever summarizatorKind changes.
    private final ObjectBinding<Summarizator> summarizatorBinding;

    private final AiPreferences aiPreferences;

    public AiSummaryParametersViewModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        setupValues();

        summarizatorBinding = Bindings.createObjectBinding(
                this::constructSummarizator,
                summarizatorKind
        );
    }

    private void setupValues() {
        this.summarizatorKind.set(aiPreferences.getSummarizatorKind());
    }

    public ListProperty<SummarizatorKind> summarizatorKindsProperty() {
        return summarizatorKinds;
    }

    public ObjectProperty<SummarizatorKind> summarizatorKindProperty() {
        return summarizatorKind;
    }

    /// Returns a reactive binding that always reflects the {@link Summarizator} built from the
    /// current summarization kind. Recomputes lazily whenever {@code summarizatorKind} changes.
    public ObservableValue<Summarizator> summarizatorProperty() {
        return summarizatorBinding;
    }

    public Summarizator constructSummarizator() {
        return SummarizatorFactory.create(
                summarizatorKind.get(),
                aiPreferences.getSummarizationChunkSystemMessageTemplate(),
                aiPreferences.getSummarizationCombineSystemMessageTemplate(),
                aiPreferences.getSummarizationFullDocumentSystemMessageTemplate()
        );
    }
}
