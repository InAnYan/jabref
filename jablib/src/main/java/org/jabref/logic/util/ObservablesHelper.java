package org.jabref.logic.util;

import java.util.List;
import java.util.concurrent.Callable;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;

/**
 * Utility methods for working with JavaFX {@link Observable} objects in non-GUI logic code.
 * <p>
 * GUI code should use {@code BindingsHelper} from {@code jabgui} instead.
 */
public final class ObservablesHelper {
    private ObservablesHelper() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static <T> ObjectBinding<T> createObjectBinding(final Callable<T> func, final List<? extends Observable> dependencies) {
        return Bindings.createObjectBinding(func, dependencies.toArray(Observable[]::new));
    }

    public static <T> void onChange(List<? extends Observable> observables, Runnable... actions) {
        observables.forEach(obs -> obs.addListener(_ -> {
            for (var action : actions) {
                action.run();
            }
        }));
    }
}

