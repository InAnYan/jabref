package org.jabref.logic.ai.ingestion;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Future;

import javafx.util.Pair;

import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.ingestion.tasks.generateembeddingsforseveral.GenerateEmbeddingsForSeveralTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddingsforseveral.GenerateEmbeddingsForSeveralTaskRequest;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.LinkedFile;

import com.google.common.collect.Comparators;

public class IngestionTaskAggregator {
    private final TaskExecutor taskExecutor;

    // TODO: It's wrong to compare by links, as links might be relative.
    private final TreeMap<LinkedFile, Pair<Future<Void>, GenerateEmbeddingsTask>> generateEmbeddingsTasks =
            new TreeMap<>(Comparator.comparing(LinkedFile::getLink));

    // TODO: It's also wrong to compare by list of files, as the group might change, and this will lead to change of the bib entry list.
    private final TreeMap<List<LinkedFile>, Pair<Future<Void>, GenerateEmbeddingsForSeveralTask>> generateEmbeddingsForSeveralTasks =
            new TreeMap<>(Comparators.lexicographical(Comparator.comparing(LinkedFile::getLink)));

    public IngestionTaskAggregator(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public synchronized Pair<Future<Void>, GenerateEmbeddingsTask> startWithFuture(GenerateEmbeddingsTaskRequest request) {
        return generateEmbeddingsTasks.computeIfAbsent(request.linkedFile(), _ -> {
            GenerateEmbeddingsTask task = new GenerateEmbeddingsTask(request);
            task.onFinished(() -> generateEmbeddingsTasks.remove(request.linkedFile()));
            Future<Void> future = taskExecutor.execute(task);
            return new Pair<>(future, task);
        });
    }

    public synchronized GenerateEmbeddingsTask start(GenerateEmbeddingsTaskRequest request) {
        return startWithFuture(request).getValue();
    }

    public synchronized Pair<Future<Void>, GenerateEmbeddingsForSeveralTask> start(GenerateEmbeddingsForSeveralTaskRequest request) {
        return generateEmbeddingsForSeveralTasks.computeIfAbsent(request.linkedFiles(), _ -> {
            GenerateEmbeddingsForSeveralTask task = new GenerateEmbeddingsForSeveralTask(this, request);
            task.onFinished(() -> generateEmbeddingsForSeveralTasks.remove(request.linkedFiles()));
            Future<Void> future = taskExecutor.execute(task);
            return new Pair<>(future, task);
        });
    }
}
