package org.jabref.gui.util;

import java.util.function.Consumer;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;

public class ListenersHelper {
    private ListenersHelper() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    @SafeVarargs
    public static <T> void onChangeNonNull(ObservableValue<T> observable, Consumer<T>... actions) {
        observable.addListener((_, _, newVal) -> {
            if (newVal != null) {
                for (var action : actions) {
                    action.accept(newVal);
                }
            }
        });
    }

    public static <T> void onChangeNonNull(ObservableValue<T> observable, Runnable... actions) {
        observable.addListener((_, _, newVal) -> {
            if (newVal != null) {
                for (var action : actions) {
                    action.run();
                }
            }
        });
    }

    @SafeVarargs
    public static <T> void onChangeNonNullWhen(
            ObservableValue<T> observable,
            ObservableValue<Boolean> condition,
            Consumer<T>... actions
    ) {
        observable.addListener((_, _, newVal) -> {
            if (newVal != null && Boolean.TRUE.equals(condition.getValue())) {
                for (var action : actions) {
                    action.accept(newVal);
                }
            }
        });
    }

    public static <T, U> void onChangeNonNullWhen(
            ObservableValue<T> observable1,
            ObservableValue<U> observable2,
            ObservableValue<Boolean> condition,
            Runnable... actions
    ) {
        InvalidationListener listener = _ -> {
            boolean obs1Present = observable1.getValue() != null;
            boolean obs2Present = observable2.getValue() != null;
            boolean isConditionMet = Boolean.TRUE.equals(condition.getValue());

            if (obs1Present && obs2Present && isConditionMet) {
                for (var action : actions) {
                    action.run();
                }
            }
        };

        observable1.addListener(listener);
        observable2.addListener(listener);
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

    public static <T> void runWhenListChanges(ListProperty<T> listProperty, Runnable... actions) {
        listProperty.addListener((ListChangeListener<T>) _ -> {
            for (Runnable action : actions) {
                action.run();
            }
        });
    }

    public static <T> void runWhenListChangesWithPrecondition(
            ListProperty<T> listProperty,
            ObservableBooleanValue condition,
            Runnable... actions
    ) {
        runWhenListChanges(listProperty, () -> {
            if (condition.get()) {
                for (Runnable action : actions) {
                    action.run();
                }
            }
        });
    }
}
