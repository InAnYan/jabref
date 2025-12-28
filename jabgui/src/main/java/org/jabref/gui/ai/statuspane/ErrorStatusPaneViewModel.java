package org.jabref.gui.ai.statuspane;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.gui.util.ExceptionsUtil;

public class ErrorStatusPaneViewModel {
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");

    private final ObjectProperty<Exception> exception = new SimpleObjectProperty<>();
    private final StringProperty exceptionString = new SimpleStringProperty("");

    private final StringProperty restartButtonText = new SimpleStringProperty("");
    private final ObjectProperty<EventHandler<ActionEvent>> onRestart = new SimpleObjectProperty<>();

    private final StringProperty cancelButtonText = new SimpleStringProperty("");
    private final ObjectProperty<EventHandler<ActionEvent>> onCancel = new SimpleObjectProperty<>();

    public ErrorStatusPaneViewModel() {
        exception.addListener((_, _, value) -> exceptionString.set(ExceptionsUtil.generateExceptionMessage(value)));
    }

    public void restart() {
        if (onRestart.get() != null) {
            onRestart.get().handle(new ActionEvent());
        }
    }

    public void cancel() {
        if (onCancel.get() != null) {
            onCancel.get().handle(new ActionEvent());
        }
    }

    public StringProperty titleProperty() {
        return title;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public ObjectProperty<Exception> exceptionProperty() {
        return exception;
    }

    public StringProperty exceptionStringProperty() {
        return exceptionString;
    }

    public StringProperty restartButtonTextProperty() {
        return restartButtonText;
    }

    public StringProperty cancelButtonTextProperty() {
        return cancelButtonText;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRestartProperty() {
        return onRestart;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onCancelProperty() {
        return onCancel;
    }
}
