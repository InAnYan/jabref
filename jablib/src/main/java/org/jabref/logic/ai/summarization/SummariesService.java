package org.jabref.logic.ai.summarization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.SummarizationAlgorithm;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryForSeveralTask;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTask;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.processingstatus.ProcessingInfo;
import org.jabref.model.ai.processingstatus.ProcessingState;
import org.jabref.model.ai.summarization.BibEntrySummary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for generating summaries of {@link BibEntry}ies.
 * Use this class in the logic and UI.
 * <p>
 * In order for summary to be stored and loaded, the {@link BibEntry} must satisfy the following requirements:
 * 1. There should exist an associated {@link BibDatabaseContext} for the {@link BibEntry}.
 * 2. The database path of the associated {@link BibDatabaseContext} must be set.
 * 3. The citation key of the {@link BibEntry} must be set and unique.
 */
public class SummariesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummariesService.class);

    private final TreeMap<BibEntry, ProcessingInfo<BibEntry, BibEntrySummary>> summariesStatusMap = new TreeMap<>(Comparator.comparing(BibEntry::getId));

    private final List<List<BibEntry>> listsUnderSummarization = new ArrayList<>();

    private final AiPreferences aiPreferences;
    private final SummariesRepository summariesRepository;
    private final ChatModel chatModel;
    private final SummarizationAlgorithm defaultSummarizationAlgorithm;
    private final BooleanProperty shutdownSignal;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    // TODO: chat model should be argument.
    public SummariesService(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            TaskExecutor taskExecutor,
            ChatModel chatModel,
            SummarizationAlgorithm defaultSummarizationAlgorithm,
            SummariesRepository summariesRepository,
            BooleanProperty shutdownSignal
    ) {
        this.aiPreferences = aiPreferences;
        this.summariesRepository = summariesRepository;
        this.chatModel = chatModel;
        this.defaultSummarizationAlgorithm = defaultSummarizationAlgorithm;
        this.shutdownSignal = shutdownSignal;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;
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
                    summarize(defaultSummarizationAlgorithm, entry, bibDatabaseContext);
                }
            });
        }

        @Subscribe
        public void listen(FieldChangedEvent e) {
            if (e.getField() == StandardField.FILE && aiPreferences.getAutoGenerateSummaries()) {
                summarize(defaultSummarizationAlgorithm, e.getBibEntry(), bibDatabaseContext);
            }
        }
    }

    /**
     * Start generating summary of a {@link BibEntry}, if it was already generated.
     * This method returns a {@link ProcessingInfo} that can be used for tracking state of the summarization.
     * Returned {@link ProcessingInfo} is related to the passed {@link BibEntry}, so if you call this method twice
     * on the same {@link BibEntry}, the method will return the same {@link ProcessingInfo}.
     */
    public ProcessingInfo<BibEntry, BibEntrySummary> summarize(SummarizationAlgorithm summarizationAlgorithm, BibEntry bibEntry, BibDatabaseContext bibDatabaseContext) {
        ProcessingInfo<BibEntry, BibEntrySummary> processingInfo = getProcessingInfo(bibEntry);

        if (processingInfo.getState() == ProcessingState.STOPPED) {
            startSummarizationTask(summarizationAlgorithm, bibEntry, bibDatabaseContext, processingInfo);
        }

        return processingInfo;
    }

    public ProcessingInfo<BibEntry, BibEntrySummary> getProcessingInfo(BibEntry entry) {
        return summariesStatusMap.computeIfAbsent(entry, _ -> new ProcessingInfo<>(entry, ProcessingState.STOPPED));
    }

    public List<ProcessingInfo<BibEntry, BibEntrySummary>> getProcessingInfo(List<BibEntry> entries) {
        return entries.stream().map(this::getProcessingInfo).toList();
    }

    public void summarize(
            SummarizationAlgorithm summarizationAlgorithm,
            StringProperty groupName,
            List<BibEntry> entries,
            BibDatabaseContext bibDatabaseContext
    ) {
        List<ProcessingInfo<BibEntry, BibEntrySummary>> result = getProcessingInfo(entries);

        if (listsUnderSummarization.contains(entries)) {
            return;
        }

        listsUnderSummarization.add(entries);

        List<ProcessingInfo<BibEntry, BibEntrySummary>> needToProcess = result.stream().filter(processingInfo -> processingInfo.getState() == ProcessingState.STOPPED).toList();
        startSummarizationTask(
                summarizationAlgorithm,
                groupName,
                needToProcess,
                bibDatabaseContext
        );
    }

    private void startSummarizationTask(
            SummarizationAlgorithm summarizationAlgorithm,
            BibEntry entry,
            BibDatabaseContext bibDatabaseContext,
            ProcessingInfo<BibEntry, BibEntrySummary> processingInfo
    ) {
        processingInfo.setState(ProcessingState.PROCESSING);

        new GenerateSummaryTask(
                filePreferences,
                chatModel,
                summariesRepository,
                summarizationAlgorithm,
                bibDatabaseContext,
                entry,
                shutdownSignal
        )
                .onSuccess(processingInfo::setSuccess)
                .onFailure(processingInfo::setException)
                .executeWith(taskExecutor);
    }

    private void startSummarizationTask(
            SummarizationAlgorithm summarizationAlgorithm,
            StringProperty groupName,
            List<ProcessingInfo<BibEntry, BibEntrySummary>> entries,
            BibDatabaseContext bibDatabaseContext
    ) {
        entries.forEach(processingInfo -> processingInfo.setState(ProcessingState.PROCESSING));

        new GenerateSummaryForSeveralTask(
                filePreferences,
                taskExecutor,
                chatModel,
                summariesRepository,
                summarizationAlgorithm,
                bibDatabaseContext,
                groupName,
                entries,
                shutdownSignal
        )
                .executeWith(taskExecutor);
    }

    /**
     * Method, similar to {@link #summarize(SummarizationAlgorithm, BibEntry, BibDatabaseContext)}, but it allows you to regenerate summary.
     */
    public void regenerateSummary(
            SummarizationAlgorithm summarizationAlgorithm,
            BibEntry bibEntry,
            BibDatabaseContext bibDatabaseContext
    ) {
        ProcessingInfo<BibEntry, BibEntrySummary> processingInfo = summarize(summarizationAlgorithm, bibEntry, bibDatabaseContext);
        processingInfo.setState(ProcessingState.PROCESSING);

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. Could not clear stored summary for regeneration");
        } else if (bibEntry.getCitationKey().isEmpty() || CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, bibEntry)) {
            LOGGER.info("No valid citation key is present. Could not clear stored summary for regeneration");
        } else {
            summariesRepository.clear(bibDatabaseContext.getDatabasePath().get(), bibEntry.getCitationKey().get());
        }

        startSummarizationTask(summarizationAlgorithm, bibEntry, bibDatabaseContext, processingInfo);
    }
}
