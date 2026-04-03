package org.jabref.logic.ai.summarization.logic;

import javafx.beans.property.ObjectProperty;

import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.ai.summarization.SummarizatorKind;

public class AiSummarizationLogic {
    private final ObjectProperty<SummarizatorKind> summarizatorKind;
    private final ObjectProperty<Summarizator> summarizator;

    public AiSummarizationLogic(ObjectProperty<SummarizatorKind> summarizatorKind, ObjectProperty<Summarizator> summarizator) {
        this.summarizatorKind = summarizatorKind;
        this.summarizator = summarizator;
    }
}
