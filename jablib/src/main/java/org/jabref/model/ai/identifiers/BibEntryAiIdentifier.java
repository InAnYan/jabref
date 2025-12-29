package org.jabref.model.ai.identifiers;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public record BibEntryAiIdentifier(BibDatabaseContext databaseContext, BibEntry entry) {
    public static List<BibEntryAiIdentifier> fromSeveral(
            BibDatabaseContext databaseContext,
            List<BibEntry> entries
    ) {
        return entries
                .stream()
                .map(entry ->
                        new BibEntryAiIdentifier(databaseContext, entry))
                .toList();
    }
}
