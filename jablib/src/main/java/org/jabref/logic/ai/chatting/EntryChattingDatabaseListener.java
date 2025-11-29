package org.jabref.logic.ai.chatting;

import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepositoryV2;
import org.jabref.model.database.BibDatabaseContext;

public class EntryChattingDatabaseListener {
    private final EntryChatHistoryRepositoryV2 entryChatHistoryRepository;

    public EntryChattingDatabaseListener(EntryChatHistoryRepositoryV2 entryChatHistoryRepository) {
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
