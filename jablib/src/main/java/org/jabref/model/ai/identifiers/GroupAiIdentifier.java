package org.jabref.model.ai.identifiers;

import java.nio.file.Path;

public record GroupAiIdentifier(Path databasePath, String groupName) {
}
