package org.jabref.logic.ai;

import org.jabref.model.database.BibDatabaseContext;

public interface AiFeature extends AutoCloseable {
    void setupDatabase(BibDatabaseContext context);
}
