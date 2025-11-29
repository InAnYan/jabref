package org.jabref.logic.ai.summarization;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.listeners.GenerateSummaryDatabaseListener;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.database.BibDatabaseContext;

public class SummarizationDatabaseListeners {
    private final GenerateSummaryDatabaseListener generateSummaryDatabaseListener;

    public SummarizationDatabaseListeners(
            AiPreferences aiPreferences,
            SummariesService summariesService,
            Summarizator summarizator
    ) {
        this.generateSummaryDatabaseListener = new GenerateSummaryDatabaseListener(
                aiPreferences,
                summariesService,
                summarizator
        );
    }

    public void setupDatabase(BibDatabaseContext databaseContext) {
        generateSummaryDatabaseListener.setupDatabase(databaseContext);
    }
}
