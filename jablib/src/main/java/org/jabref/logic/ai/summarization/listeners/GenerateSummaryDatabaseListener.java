package org.jabref.logic.ai.summarization.listeners;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.DatabaseListener;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTaskRequest;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;

public class GenerateSummaryDatabaseListener implements DatabaseListener {
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final ChatModel chatModel;
    private final SummariesRepository summariesRepository;
    private final SummarizationTaskAggregator summarizationTaskAggregator;
    private final Summarizator summarizator;

    public GenerateSummaryDatabaseListener(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            ChatModel chatModel,
            SummariesRepository summariesRepository,
            SummarizationTaskAggregator summarizationTaskAggregator,
            Summarizator summarizator
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.chatModel = chatModel;
        this.summariesRepository = summariesRepository;
        this.summarizationTaskAggregator = summarizationTaskAggregator;
        this.summarizator = summarizator;
    }

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        // GC was eating the listeners, so we have to fall back to the event bus.
        context.getDatabase().registerListener(new EntriesChangedListener(context));
    }

    private class EntriesChangedListener {
        private final BibDatabaseContext context;

        public EntriesChangedListener(BibDatabaseContext context) {
            this.context = context;
        }

        @Subscribe
        public void listen(EntriesAddedEvent e) {
            e.getBibEntries().forEach(entry -> {
                if (aiPreferences.getAutoGenerateSummaries()) {
                    summarizationTaskAggregator.start(new GenerateSummaryTaskRequest(
                            filePreferences,
                            chatModel,
                            summariesRepository,
                            summarizator,
                            context,
                            entry,
                            false
                    ));
                }
            });
        }

        @Subscribe
        public void listen(FieldChangedEvent e) {
            if (e.getField() == StandardField.FILE && aiPreferences.getAutoGenerateSummaries()) {
                summarizationTaskAggregator.start(new GenerateSummaryTaskRequest(
                        filePreferences,
                        chatModel,
                        summariesRepository,
                        summarizator,
                        context,
                        e.getBibEntry(),
                        false
                ));
            }
        }
    }
}
