package org.jabref.model.ai.chatting;

import java.nio.file.Path;

public class EntryChatHistoryIdentifier implements ChatHistoryIdentifier {
    private final Path databasePath;
    private final String entryId;

    public EntryChatHistoryIdentifier(Path databasePath, String entryId) {
        this.databasePath = databasePath;
        this.entryId = entryId;
    }

    @Override
    public ChatHistoryType getType() {
        return ChatHistoryType.WITH_ENTRY;
    }

    @Override
    public String getName() {
        return databasePath.toString() + "/" + entryId;
    }
}
