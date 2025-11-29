package org.jabref.logic.ai.chatting.listeners;

import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepository;
import org.jabref.model.database.BibDatabaseContext;

public class EntryChattingDatabaseListener {
    private final EntryChatHistoryRepository entryChatHistoryRepository;

    public EntryChattingDatabaseListener(EntryChatHistoryRepository entryChatHistoryRepository) {
        this.entryChatHistoryRepository = entryChatHistoryRepository;
    }

    public void setupDatabase(BibDatabaseContext databaseContext) {
        databaseContext
                .getDatabase()
                .getEntries()
                .forEach(entry ->
                        entry.registerListener(new CitationKeyChangeListener(entryChatHistoryRepository, databaseContext))
                );
    }
}
