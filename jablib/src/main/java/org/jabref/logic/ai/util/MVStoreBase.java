package org.jabref.logic.ai.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.util.NotificationService;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for components backed by an H2 {@link MVStore}.
 * <p>
 * The constructor attempts to open a file-backed MVStore at the given path,
 * creating parent directories if necessary. If directory creation fails or the
 * store cannot be opened, it transparently falls back to an in-memory store.
 * Errors are logged and reported via the provided {@link NotificationService}.
 * <p>
 * This class is intentionally designed to be resilient: callers and subclasses
 * can assume that an {@link MVStore} is always available after construction.
 * This avoids spreading error handling, null checks, and recovery logic across
 * higher-level code, keeping repository implementations simpler and more
 * focused on their domain logic.
 * <p>
 * The abstract error message methods allow subclasses to provide
 * context-specific and localized messages. Multiple repositories rely on this
 * base class, and each may need to report a more precise reason (and wording)
 * for why a particular store could not be opened.
 * <p>
 * Implements {@link AutoCloseable}; callers must invoke {@link #close()} to
 * release the underlying store.
 */
public abstract class MVStoreBase implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MVStoreBase.class);

    protected MVStore mvStore;

    public MVStoreBase(@NonNull Path path, NotificationService dialogService) {
        Path mvStorePath = path;

        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            LOGGER.error(errorMessageForOpening(), e);
            dialogService.notify(errorMessageForOpeningLocalized());
            mvStorePath = null;
        }

        try {
            this.mvStore = new MVStore.Builder()
                    .fileName(mvStorePath == null ? null : mvStorePath.toString())
                    .open();
        } catch (MVStoreException e) {
            this.mvStore = new MVStore.Builder()
                    .fileName(null) // creates an in memory store
                    .open();
            LOGGER.error(errorMessageForOpening(), e);
        }
    }

    public void close() {
        mvStore.close();
    }

    protected abstract String errorMessageForOpening();

    protected abstract String errorMessageForOpeningLocalized();
}
