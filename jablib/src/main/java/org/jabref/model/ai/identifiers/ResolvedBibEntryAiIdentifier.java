package org.jabref.model.ai.identifiers;

import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;

public record ResolvedBibEntryAiIdentifier(Path databasePath, String citationKey) {
    @Override
    public @NotNull String toString() {
        return databasePath.toString() + "/" + citationKey;
    }
}
