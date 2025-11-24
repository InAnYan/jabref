package org.jabref.logic.ai.summarization.algorithms;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.util.ProgressCounter;

public interface SummarizationAlgorithm {
    String summarize(
            String text,
            ProgressCounter progressCounter,
            ReadOnlyBooleanProperty shutdownSignal
    ) throws InterruptedException;
}
