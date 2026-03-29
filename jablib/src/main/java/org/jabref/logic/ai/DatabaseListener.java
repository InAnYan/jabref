package org.jabref.logic.ai;

import org.jabref.model.database.BibDatabaseContext;

public interface DatabaseListener {
    void setupDatabase(BibDatabaseContext databaseContext);
}

