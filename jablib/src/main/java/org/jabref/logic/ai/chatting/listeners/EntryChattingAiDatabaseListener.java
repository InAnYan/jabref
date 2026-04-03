package org.jabref.logic.ai.chatting.listeners;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.AiDatabaseListener;
import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.chatting.EntryChatHistoryIdentifier;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.InternalField;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntryChattingAiDatabaseListener implements AiDatabaseListener {
    private final ChatHistoryRepository chatHistoryRepository;

    public EntryChattingAiDatabaseListener(ChatHistoryRepository chatHistoryRepository) {
        this.chatHistoryRepository = chatHistoryRepository;
    }

    @Override
    public void setupDatabase(BibDatabaseContext databaseContext) {
        databaseContext
                .getDatabase()
                .getEntries()
                .forEach(entry ->
                        entry.registerListener(new CitationKeyChangeListener(databaseContext))
                );
    }

    @Override
    public void close() throws Exception {
        // Nothing to close.
    }

    private class CitationKeyChangeListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(CitationKeyChangeListener.class);

        private final BibDatabaseContext bibDatabaseContext;

        public CitationKeyChangeListener(
                BibDatabaseContext bibDatabaseContext
        ) {
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
            // TODO: I think transferHistory methods could be generalized somehow.

            Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();

            if (databasePath.isEmpty()) {
                LOGGER.warn("Could not transfer chat history of entry {} (old key: {}): database path is empty.", newCitationKey, oldCitationKey);
                return;
            }

            EntryChatHistoryIdentifier oldIdentifier = new EntryChatHistoryIdentifier(databasePath.get(), oldCitationKey);
            EntryChatHistoryIdentifier newIdentifier = new EntryChatHistoryIdentifier(databasePath.get(), newCitationKey);

            List<ChatMessage> chatHistory = chatHistoryRepository.getAllMessages(oldIdentifier);

            chatHistoryRepository.clear(oldIdentifier);
            chatHistoryRepository.clear(newIdentifier);

            chatHistory.forEach(record -> chatHistoryRepository.addMessage(newIdentifier, record));
        }
    }
}
