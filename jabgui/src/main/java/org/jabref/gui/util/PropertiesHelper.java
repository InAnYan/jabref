package org.jabref.gui.util;

import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class PropertiesHelper {
    private PropertiesHelper() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static void handle(ObjectProperty<EventHandler<ActionEvent>> handler) {
        if (handler.get() != null) {
            handler.get().handle(null);
        }
    }
}
