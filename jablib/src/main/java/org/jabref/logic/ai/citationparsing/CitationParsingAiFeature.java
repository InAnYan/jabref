package org.jabref.logic.ai.citationparsing;

import org.jabref.logic.ai.AiFeature;
import org.jabref.model.database.BibDatabaseContext;

public class CitationParsingAiFeature implements AiFeature {
    @Override
    public void setupDatabase(BibDatabaseContext context) {
        // No listeners.
    }

    @Override
    public void close() {
        // Nothing to close.
    }
}
