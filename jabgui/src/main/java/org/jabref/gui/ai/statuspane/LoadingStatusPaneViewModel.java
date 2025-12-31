package org.jabref.gui.ai.statuspane;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.PropertiesHelper;

public class LoadingStatusPaneViewModel extends AbstractViewModel {
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");

    private final ObjectProperty<EventHandler<ActionEvent>> onCancel = new SimpleObjectProperty<>();

    public void cancel() {
        PropertiesHelper.handle(onCancel);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onCancelProperty() {
        return onCancel;
    }
}
