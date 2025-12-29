package org.jabref.model.ai.chatting;

import java.nio.file.Path;

public record GroupChatHistoryIdentifier(Path databasePath, String groupName) implements ChatHistoryIdentifier {
    @Override
    public String toStringRepresentation() {
        return databasePath.toString() + "/" + groupName;
    }
}
