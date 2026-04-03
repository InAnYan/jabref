package org.jabref.logic.util;

import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public final class CitationKeyCheck {
    private CitationKeyCheck() {
        throw new UnsupportedOperationException("Cannot instantiate a utility class");
    }

    public static boolean citationKeyIsPresentAndUnique(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        return !hasEmptyCitationKey(bibEntry) && bibEntry.getCitationKey().map(key -> citationKeyIsUnique(bibDatabaseContext, key)).orElse(false);
    }

    public static boolean hasEmptyCitationKey(BibEntry bibEntry) {
        return bibEntry.getCitationKey().map(String::isEmpty).orElse(true);
    }

    public static boolean citationKeyIsUnique(BibDatabaseContext bibDatabaseContext, String citationKey) {
        return bibDatabaseContext.getDatabase().getNumberOfCitationKeyOccurrences(citationKey) == 1;
    }

    public static boolean citationKeyIsUnique(FullBibEntry identifier) {
        return identifier.entry().getCitationKey().map(key -> citationKeyIsUnique(identifier.databaseContext(), key)).orElse(false);
    }
}
