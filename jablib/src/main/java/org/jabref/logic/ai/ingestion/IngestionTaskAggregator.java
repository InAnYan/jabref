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

public class IngestionTaskAggregator {
    private final TaskExecutor taskExecutor;

    // TODO: It's wrong to compare by links, as links might be relative.
    private final TreeMap<LinkedFile, Pair<Future<Void>, GenerateEmbeddingsTask>> generateEmbeddingsTasks =
            new TreeMap<>(Comparator.comparing(LinkedFile::getLink));

    private final TreeMap<List<LinkedFile>, Pair<Future<Void>, GenerateEmbeddingsForSeveralTask>> generateEmbeddingsForSeveralTasks =
            new TreeMap<>((a, b) -> {
                if (a.size() != b.size()) {
                    return Integer.compare(a.size(), b.size());
                }

                for (int i = 0; i < a.size(); i++) {
                    int cmp = a.get(i).getLink().compareTo(b.get(i).getLink());
                    if (cmp != 0) {
                        return cmp;
                    }
                }

                return 0;
            });

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
