package org.jabref.model.ai.identifiers;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public record FullBibEntryAiIdentifier(BibDatabaseContext databaseContext, BibEntry entry) {
}
