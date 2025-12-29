package org.jabref.model.ai.chatting;

import java.nio.file.Path;

public record EntryChatHistoryIdentifier(Path databasePath, String entryId) implements ChatHistoryIdentifier {
    @Override
    public String toStringRepresentation() {
        return databasePath.toString() + "/" + entryId;
    }
}
