package org.jabref.model.ai.identifiers;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public record BibEntryAiIdentifier(BibDatabaseContext databaseContext, BibEntry entry) {
    public static Stream<BibEntryAiIdentifier> fromSeveral(
            BibDatabaseContext databaseContext,
            Stream<BibEntry> entries
    ) {
        return entries
                .map(entry ->
                        new BibEntryAiIdentifier(databaseContext, entry));
    }

    public static List<BibEntryAiIdentifier> fromSeveral(
            BibDatabaseContext databaseContext,
            List<BibEntry> entries
    ) {
        return fromSeveral(databaseContext, entries.stream()).toList();
    }
}
