package org.jabref.logic.ai.summarization.listeners;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.SummariesService;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;

public class GenerateSummaryDatabaseListener {
    private final AiPreferences aiPreferences;
    private final SummariesService summariesService;
    private final Summarizator summarizator;

    public GenerateSummaryDatabaseListener(
            AiPreferences aiPreferences,
            SummariesService summariesService,
            Summarizator summarizator
    ) {
        this.aiPreferences = aiPreferences;
        this.summariesService = summariesService;
        this.summarizator = summarizator;
    }

    public void setupDatabase(BibDatabaseContext bibDatabaseContext) {
        // GC was eating the listeners, so we have to fall back to the event bus.
        bibDatabaseContext.getDatabase().registerListener(new EntriesChangedListener(bibDatabaseContext));
    }

    private class EntriesChangedListener {
        private final BibDatabaseContext bibDatabaseContext;

        public EntriesChangedListener(BibDatabaseContext bibDatabaseContext) {
            this.bibDatabaseContext = bibDatabaseContext;
        }

        @Subscribe
        public void listen(EntriesAddedEvent e) {
            e.getBibEntries().forEach(entry -> {
                if (aiPreferences.getAutoGenerateSummaries()) {
                    summariesService.summarize(summarizator, entry, bibDatabaseContext);
                }
            });
        }

        @Subscribe
        public void listen(FieldChangedEvent e) {
            if (e.getField() == StandardField.FILE && aiPreferences.getAutoGenerateSummaries()) {
                summariesService.summarize(summarizator, e.getBibEntry(), bibDatabaseContext);
            }
        }
    }
}
