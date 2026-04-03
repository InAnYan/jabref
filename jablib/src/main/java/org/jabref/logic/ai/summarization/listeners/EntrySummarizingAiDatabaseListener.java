package org.jabref.logic.ai.summarization.listeners;

import java.util.Optional;

import org.jabref.logic.ai.AiDatabaseListener;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.AiSummaryIdentifier;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.InternalField;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntrySummarizingAiDatabaseListener implements AiDatabaseListener {
    private final SummariesRepository summariesRepository;

    public EntrySummarizingAiDatabaseListener(SummariesRepository summariesRepository) {
        this.summariesRepository = summariesRepository;
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
    public void close() {
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

            transferSummary(bibDatabaseContext, e.getBibEntry(), e.getOldValue(), e.getNewValue());
        }

        private void transferSummary(BibDatabaseContext bibDatabaseContext, BibEntry entry, String oldCitationKey, String newCitationKey) {
            // TODO: This method does not check if the citation key is valid.

            Optional<String> aiLibraryId = bibDatabaseContext.getMetaData().getAiLibraryId();

            if (aiLibraryId.isEmpty()) {
                LOGGER.warn("Could not transfer the summary of entry {} (old key: {}): AI library ID is empty.", newCitationKey, oldCitationKey);
                return;
            }

            AiSummaryIdentifier oldIdentifier = new AiSummaryIdentifier(aiLibraryId.get(), oldCitationKey);
            AiSummaryIdentifier newIdentifier = new AiSummaryIdentifier(aiLibraryId.get(), newCitationKey);

            Optional<AiSummary> summary = summariesRepository.get(oldIdentifier);

            if (summary.isPresent()) {
                summariesRepository.set(newIdentifier, summary.get());
                summariesRepository.clear(oldIdentifier);
            }
        }
    }
}
