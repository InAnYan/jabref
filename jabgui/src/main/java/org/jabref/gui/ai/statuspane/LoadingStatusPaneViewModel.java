package org.jabref.gui.ai.statuspane;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LoadingStatusPaneViewModel {
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");

    public StringProperty titleProperty() {
        return title;
    }

    public StringProperty descriptionProperty() {
        return description;
    }
}
