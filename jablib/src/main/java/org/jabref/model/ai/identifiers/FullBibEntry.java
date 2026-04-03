package org.jabref.model.ai.identifiers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public record FullBibEntry(BibDatabaseContext databaseContext, BibEntry entry) {
    public static Stream<FullBibEntry> fromSeveral(
            BibDatabaseContext databaseContext,
            Stream<BibEntry> entries
    ) {
        return entries
                .map(entry ->
                        new FullBibEntry(databaseContext, entry));
    }

    public static List<FullBibEntry> fromSeveral(
            BibDatabaseContext databaseContext,
            List<BibEntry> entries
    ) {
        return fromSeveral(databaseContext, entries.stream()).toList();
    }

    public static Optional<BibEntry> findEntryByLink(List<FullBibEntry> entries, String link) {
        return entries
                .stream()
                .flatMap(identifier ->
                        identifier
                                .databaseContext()
                                .getEntries()
                                .stream()
                                .filter(entry ->
                                        entry
                                                .getFiles()
                                                .stream()
                                                .anyMatch(file ->
                                                        file
                                                                .getLink().
                                                                equals(link)
                                                )
                                )
                )
                .findFirst();
    }

    public static Optional<BibEntry> findEntryByLink(FullBibEntry entry, String link) {
        return findEntryByLink(List.of(entry), link);
    }
}
