package org.jabref.model.ai.identifiers;

import java.nio.file.Path;

public record BibEntryAiIdentifier(Path databasePath, String citationKey) {
}
