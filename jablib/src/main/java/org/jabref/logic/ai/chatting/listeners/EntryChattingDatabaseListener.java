package org.jabref.logic.ai.chatting.listeners;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.InternalField;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    static class CitationKeyChangeListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(CitationKeyChangeListener.class);

        private final EntryChatHistoryRepository entryChatHistoryRepository;
        private final BibDatabaseContext bibDatabaseContext;

        public CitationKeyChangeListener(EntryChatHistoryRepository entryChatHistoryRepository, BibDatabaseContext bibDatabaseContext) {
            this.entryChatHistoryRepository = entryChatHistoryRepository;
            this.bibDatabaseContext = bibDatabaseContext;
        }

        @Subscribe
        void listen(FieldChangedEvent e) {
            if (e.getField() != InternalField.KEY_FIELD) {
                return;
            }

            transferHistory(bibDatabaseContext, e.getBibEntry(), e.getOldValue(), e.getNewValue());
        }

        private void transferHistory(BibDatabaseContext bibDatabaseContext, BibEntry entry, String oldCitationKey, String newCitationKey) {
            // TODO: This method does not check if the citation key is valid.

            Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();

            if (databasePath.isEmpty()) {
                LOGGER.warn("Could not transfer chat history of entry {} (old key: {}): database path is empty.", newCitationKey, oldCitationKey);
                return;
            }

            BibEntryAiIdentifier oldIdentifier = new BibEntryAiIdentifier(databasePath.get(), oldCitationKey);
            BibEntryAiIdentifier newIdentifier = new BibEntryAiIdentifier(databasePath.get(), newCitationKey);

            List<ChatHistoryRecordV2> chatHistory = entryChatHistoryRepository.getAllMessages(oldIdentifier);

            entryChatHistoryRepository.clear(oldIdentifier);
            entryChatHistoryRepository.clear(newIdentifier);

            chatHistory.forEach(record -> entryChatHistoryRepository.addMessage(newIdentifier, record));
        }
    }
}
