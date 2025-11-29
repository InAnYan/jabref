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
            new TreeMap<>(new Comparator<List<BibEntry>>() {
        @Override
        public int compare(List<BibEntry> list1, List<BibEntry> list2) {
            if (list1 == list2) return true;
            if (list1 == null || list2 == null) return false;
            if (list1.size() != list2.size()) return false;

            // 2. Map to IDs and compare
            List<String> ids1 = list1.stream().map(BibEntry::getId).toList();
            List<String> ids2 = list2.stream().map(BibEntry::getId).toList();

            return ids1.equals(ids2);
        }
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
