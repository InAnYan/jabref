package org.jabref.logic.ai.templates;

import org.jabref.logic.ai.AiFeature;
import org.jabref.model.database.BibDatabaseContext;

public class TemplatesAiFeature implements AiFeature {
    @Override
    public void setupDatabase(BibDatabaseContext context) {
        // No listeners.
    }


    @Override
    public void close() {
        // Nothing to close.
    }
}
