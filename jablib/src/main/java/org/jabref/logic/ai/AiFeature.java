package org.jabref.logic.ai;

import java.util.ArrayList;

import org.jabref.model.database.BibDatabaseContext;

public abstract class AiFeature implements DatabaseListener, AutoCloseable {
    protected final ArrayList<DatabaseListener> databaseListeners = new ArrayList<>();

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        databaseListeners.forEach(listener -> listener.setupDatabase(context));
    }
}
