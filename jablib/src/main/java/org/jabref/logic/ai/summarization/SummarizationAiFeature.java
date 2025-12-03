package org.jabref.logic.ai.summarization;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.listeners.GenerateSummaryDatabaseListener;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.repositories.MVStoreSummariesRepository;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.templates.AiTemplatesFactory;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

public class SummarizationAiFeature implements AiFeature {
    private static final String SUMMARIES_FILE_NAME = "summaries.mv";

    private final MVStoreSummariesRepository mvStoreSummariesRepository;

    private final CurrentSummarizator currentSummarizator;

    private final GenerateSummaryDatabaseListener generateSummaryDatabaseListener;
    private final SummarizationTaskAggregator summarizationTaskAggregator;

    public SummarizationAiFeature(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            ChatModel chatModel,
            AiTemplatesFactory aiTemplatesFactory,
            ReadOnlyBooleanProperty shutdownSignal,
            TaskExecutor taskExecutor,
            NotificationService notificationService
    ) {
        this.mvStoreSummariesRepository = new MVStoreSummariesRepository(
                notificationService, Directories.getAiFilesDirectory().resolve(SUMMARIES_FILE_NAME)
        );

        this.currentSummarizator = new CurrentSummarizator(aiPreferences, aiTemplatesFactory);

        this.summarizationTaskAggregator = new SummarizationTaskAggregator(taskExecutor);
        this.generateSummaryDatabaseListener = new GenerateSummaryDatabaseListener(
                aiPreferences,
                filePreferences,
                chatModel,
                mvStoreSummariesRepository,
                summarizationTaskAggregator,
                currentSummarizator,
                shutdownSignal
        );
    }

    public void setupDatabase(BibDatabaseContext context) {
        generateSummaryDatabaseListener.setupDatabase(context);
    }

    public SummarizationTaskAggregator getTaskAggregator() {
        return summarizationTaskAggregator;
    }

    public Summarizator getCurrentSummarizator() {
        return currentSummarizator;
    }

    public SummariesRepository getSummariesRepository() {
        return mvStoreSummariesRepository;
    }

    @Override
    public void close() throws Exception {
        this.mvStoreSummariesRepository.close();
    }
}
