package org.jabref.logic.ai.util;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.util.ProgressCounter;

public record LongTaskInfo(ProgressCounter progressCounter, ReadOnlyBooleanProperty shutdownSignal) {
    public static LongTaskInfo empty() {
        return new LongTaskInfo(new ProgressCounter(), new SimpleBooleanProperty(false));
    }
}
