package org.jabref.logic.ai.summarization;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.listeners.GenerateSummaryAiDatabaseListener;
import org.jabref.logic.ai.summarization.repositories.MVStoreSummariesRepository;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

public class SummarizationAiFeature implements AiFeature {
    private static final String SUMMARIES_FILE_NAME = "summaries.mv";

    private final MVStoreSummariesRepository mvStoreSummariesRepository;

    private final SummarizationTaskAggregator summarizationTaskAggregator;

    private final GenerateSummaryAiDatabaseListener generateSummaryAiDatabaseListener;

    public SummarizationAiFeature(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            TaskExecutor taskExecutor,
            NotificationService notificationService
    ) {
        this.mvStoreSummariesRepository = new MVStoreSummariesRepository(
                notificationService, Directories.getAiFilesDirectory().resolve(SUMMARIES_FILE_NAME)
        );

        this.summarizationTaskAggregator = new SummarizationTaskAggregator(taskExecutor);

        this.generateSummaryAiDatabaseListener = new GenerateSummaryAiDatabaseListener(
                aiPreferences,
                filePreferences,
                mvStoreSummariesRepository,
                summarizationTaskAggregator
        );
    }

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        generateSummaryAiDatabaseListener.setupDatabase(context);
    }

    public SummarizationTaskAggregator getTaskAggregator() {
        return summarizationTaskAggregator;
    }

    public SummariesRepository getSummariesRepository() {
        return mvStoreSummariesRepository;
    }

    @Override
    public void close() throws Exception {
        generateSummaryAiDatabaseListener.close();
        mvStoreSummariesRepository.close();
    }
}
