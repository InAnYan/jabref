package org.jabref.logic.ai.chatting;

import org.jabref.logic.ai.chatting.listeners.EntryChattingDatabaseListener;
import org.jabref.logic.ai.chatting.listeners.GroupChattingDatabaseListener;
import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.GroupChatHistoryRepository;
import org.jabref.model.database.BibDatabaseContext;

public class ChattingDatabaseListeners {
    private final EntryChattingDatabaseListener entryChattingDatabaseListener;
    private final GroupChattingDatabaseListener groupChattingDatabaseListener;

    public ChattingDatabaseListeners(
            EntryChatHistoryRepository entryChatHistoryRepository,
            GroupChatHistoryRepository groupChatHistoryRepository
    ) {
        this.entryChattingDatabaseListener = new EntryChattingDatabaseListener(
                entryChatHistoryRepository
        );
        this.groupChattingDatabaseListener = new GroupChattingDatabaseListener(
                groupChatHistoryRepository
        );
    }

    public void setupDatabase(BibDatabaseContext databaseContext) {
        entryChattingDatabaseListener.setupDatabase(databaseContext);
        groupChattingDatabaseListener.setupDatabase(databaseContext);
    }
}
