package org.jabref.logic.ai.summarization.tasks.generatesummaryforseveral;

import java.util.List;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTaskRequest;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public record GenerateSummaryForSeveralTaskRequest(
        FilePreferences filePreferences,
        SummarizationTaskAggregator summarizationTaskAggregator,
        TaskExecutor taskExecutor,
        ChatModel chatModel,
        SummariesRepository summariesRepository,
        Summarizator summarizator,
        BibDatabaseContext bibDatabaseContext,
        StringProperty groupName,
        List<BibEntry> entries,
        ReadOnlyBooleanProperty shutdownSignals
) {
    public GenerateSummaryTaskRequest toSingle(BibEntry entry) {
        return new GenerateSummaryTaskRequest(
                filePreferences,
                chatModel,
                summariesRepository,
                summarizator,
                bibDatabaseContext,
                entry,
                shutdownSignals
        );
    }
}
