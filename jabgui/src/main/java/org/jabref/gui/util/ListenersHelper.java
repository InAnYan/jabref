package org.jabref.gui.util;

import java.util.function.Consumer;

import javafx.beans.property.ListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;

public class ListenersHelper {
    private ListenersHelper() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    @SafeVarargs
    public static <T> void onChangeNonNull(ObservableValue<T> observable, Consumer<T>... actions) {
        observable.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                for (var action : actions) {
                    action.accept(newVal);
                }
            }
        });
    }

    public static <T> void onListContentsChange(ListProperty<T> listProperty, Consumer<T> onAdded, Consumer<T> onRemoved) {
        listProperty.addListener((ListChangeListener<T>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    c.getRemoved().forEach(onRemoved);
                }
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(onAdded);
                }
            }
        });
    }

    public static void runWhenTrue(ObservableValue<Boolean> condition, Runnable action) {
        condition.addListener((_, _, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                action.run();
            }
        });
    }
}
