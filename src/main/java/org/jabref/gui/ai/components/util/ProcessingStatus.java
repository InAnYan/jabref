package org.jabref.gui.ai.components.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.entry.LinkedFile;

public class ProcessingStatus {
    private final LinkedFile linkedFile;
    private final ObjectProperty<ProcessingState> processingState;
    private final StringProperty message;

    public ProcessingStatus(LinkedFile linkedFile, ProcessingState processingState, String message) {
        this.linkedFile = linkedFile;
        this.processingState = new SimpleObjectProperty<>(processingState);
        this.message = new SimpleStringProperty(message);
    }

    public LinkedFile getLinkedFile() {
        return linkedFile;
    }

    public ObjectProperty<ProcessingState> processingState() {
        return processingState;
    }

    public ProcessingState getProcessingStateProperty() {
        return processingState.get();
    }

    public void setProcessingState(ProcessingState processingState) {
        this.processingState.set(processingState);
    }

    public StringProperty messageProperty() {
        return message;
    }

    public String getMessage() {
        return message.get();
    }

    public void setMessage(String message) {
        this.message.set(message);
    }

    public String getPath() {
        return linkedFile.getLink();
    }
}
