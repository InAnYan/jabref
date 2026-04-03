package org.jabref.model.ai.identifiers;

import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;

public record ResoledGroup(Path databasePath, String groupName) {
    @Override
    public @NotNull String toString() {
        return databasePath.toString() + "/" + groupName;
    }
}
