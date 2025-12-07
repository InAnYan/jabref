package org.jabref.model.ai.chatting;

import java.nio.file.Path;

public class GroupChatHistoryIdentifier implements ChatHistoryIdentifier {
    private final Path databasePath;
    private final String groupName;

    public GroupChatHistoryIdentifier(Path databasePath, String groupName) {
        this.databasePath = databasePath;
        this.groupName = groupName;
    }

    @Override
    public ChatHistoryType getType() {
        return ChatHistoryType.WITH_GROUP;
    }

    @Override
    public String getName() {
        return databasePath.toString() + "/" + groupName;
    }
}
