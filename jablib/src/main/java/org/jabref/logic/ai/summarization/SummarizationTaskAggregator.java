package org.jabref.logic.ai.summarization;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTaskRequest;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.processingstatus.ProcessingInfo;
import org.jabref.model.ai.summarization.BibEntrySummary;
import org.jabref.model.entry.BibEntry;

public class SummarizationTaskAggregator {
    private final TaskExecutor taskExecutor;

    private final TreeMap<BibEntry, ProcessingInfo<BibEntry, BibEntrySummary>> generateSummaryTasks =
            new TreeMap<>(Comparator.comparing(BibEntry::getId));

    private final TreeMap<List<BibEntry>, ProcessingInfo<BibEntry, Void>> generateSummaryForSeveralTasks =
            new TreeMap<>((a, b) -> {
                if (a.size() != b.size()) {
                    return Integer.compare(a.size(), b.size());
                }

                for (int i = 0; i < a.size(); i++) {
                    int cmp = a.get(i).getId().compareTo(b.get(i).getId());
                    if (cmp != 0) {
                        return cmp;
                    }
                }

                return 0;
            });

    public SummarizationTaskAggregator(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public synchronized GenerateSummaryTask start(GenerateSummaryTaskRequest request) {
        return tasks.computeIfAbsent(request.entry(), _ -> {
            GenerateSummaryTask task = new GenerateSummaryTask(request);
            taskExecutor.execute(task);
            return task;
        });
    }
}
