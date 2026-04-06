package org.jabref.logic.util;

import javafx.beans.Observable;

/**
 * Utility methods for working with JavaFX {@link Observable} objects in non-GUI logic code.
 * <p>
 * GUI code should use {@code BindingsHelper} from {@code jabgui} instead.
 */
public final class ObservablesHelper {
    private ObservablesHelper() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /**
     * Registers {@code runnable} as an invalidation listener on every one of the given
     * {@code observables} and also invokes it once immediately (to initialise state).
     *
     * <p>Example usage:
     * <pre>{@code
     * ObservablesHelper.subscribeToChanges(
     *         this::rebuild,
     *         aiPreferences.enableAiProperty(),
     *         aiPreferences.embeddingModelProperty()
     * );
     * }</pre>
     */
    public static void subscribeToChanges(Runnable runnable, Observable... observables) {
        for (Observable observable : observables) {
            observable.addListener(_ -> runnable.run());
        }
        runnable.run();
    }
}

