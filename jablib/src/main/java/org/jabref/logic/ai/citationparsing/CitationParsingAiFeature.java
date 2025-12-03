package org.jabref.logic.ai.citationparsing;

import org.jabref.logic.ai.AiFeature;
import org.jabref.model.database.BibDatabaseContext;

public class CitationParsingAiFeature implements AiFeature {
    @Override
    public void setupDatabase(BibDatabaseContext context) {
        // Nothing to listen for.
    }

    @Override
    public void close() throws Exception {
        // Nothing to close.
    }
}
