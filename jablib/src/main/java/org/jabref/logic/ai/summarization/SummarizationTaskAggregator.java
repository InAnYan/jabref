package org.jabref.logic.ai.summarization;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Future;

import javafx.util.Pair;

import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTaskRequest;
import org.jabref.logic.ai.summarization.tasks.generatesummaryforseveral.GenerateSummaryForSeveralTask;
import org.jabref.logic.ai.summarization.tasks.generatesummaryforseveral.GenerateSummaryForSeveralTaskRequest;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.summarization.BibEntrySummary;
import org.jabref.model.entry.BibEntry;

import com.google.common.collect.Comparators;

public class SummarizationTaskAggregator {
    private final TaskExecutor taskExecutor;

    private final TreeMap<BibEntry, Pair<Future<BibEntrySummary>, GenerateSummaryTask>> generateSummaryTasks =
            new TreeMap<>(Comparator.comparing(BibEntry::getId));

    private final TreeMap<List<BibEntry>, Pair<Future<Void>, GenerateSummaryForSeveralTask>> generateSummaryForSeveralTasks =
            new TreeMap<>(Comparators.lexicographical(Comparator.comparing(BibEntry::getId)));

    public SummarizationTaskAggregator(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public synchronized Pair<Future<BibEntrySummary>, GenerateSummaryTask> startWithFuture(GenerateSummaryTaskRequest request) {
        return generateSummaryTasks.computeIfAbsent(request.entry(), _ -> {
            GenerateSummaryTask task = new GenerateSummaryTask(request);
            task.onFinished(() -> generateSummaryTasks.remove(request.entry()));
            Future<BibEntrySummary> future = taskExecutor.execute(task);
            return new Pair<>(future, task);
        });
    }

    public synchronized GenerateSummaryTask start(GenerateSummaryTaskRequest request) {
        return startWithFuture(request).getValue();
    }

    public synchronized Optional<GenerateSummaryTask> getTask(BibEntry entry) {
        return Optional.ofNullable(generateSummaryTasks.get(entry)).map(Pair::getValue);
    }

    public synchronized Pair<Future<Void>, GenerateSummaryForSeveralTask> start(GenerateSummaryForSeveralTaskRequest request) {
        return generateSummaryForSeveralTasks.computeIfAbsent(request.entries(), _ -> {
            GenerateSummaryForSeveralTask task = new GenerateSummaryForSeveralTask(request);
            task.onFinished(() -> generateSummaryTasks.remove(request.entries()));
            Future<Void> future = taskExecutor.execute(task);
            return new Pair<>(future, task);
        });
    }
}
