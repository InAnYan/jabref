package org.jabref.logic.ai.util;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.util.ProgressCounter;

public record LongTaskInfo(ProgressCounter progressCounter, ReadOnlyBooleanProperty shutdownSignal) {
}
