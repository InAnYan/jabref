package org.jabref.gui.ai.chat;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.model.entry.LinkedFile;

public class AiIngestionViewModel extends AbstractViewModel {
    public enum Status {
        PENDING,
        PROCESSING,
        ERROR_WHILE_PROCESSING,
        INGESTED,
        CANCELLED
    }

    public record IngestionState(
            Status status,
            Exception error
    ) {
    }

    private final MapProperty<LinkedFile, IngestionState> ingestionStateMap = new SimpleMapProperty<>(FXCollections.observableHashMap());

    public void addTask(GenerateEmbeddingsTask task) {
        task.statusProperty().addListener(_ -> processTask(task));
        processTask(task);
    }

    private void processTask(GenerateEmbeddingsTask task) {
        LinkedFile file = task.getLinkedFile();

        switch (task.getStatus()) {
            case SUCCESS ->
                    ingestionStateMap.put(file, new IngestionState(Status.INGESTED, null));
            case ERROR ->
                    ingestionStateMap.put(file, new IngestionState(Status.ERROR_WHILE_PROCESSING, task.getException()));
            case PENDING ->
                    ingestionStateMap.put(file, new IngestionState(Status.PENDING, null));
            case PROCESSING ->
                    ingestionStateMap.put(file, new IngestionState(Status.PROCESSING, null));
            case CANCELLED ->
                    ingestionStateMap.put(file, new IngestionState(Status.CANCELLED, null));
        }
    }

    public MapProperty<LinkedFile, IngestionState> ingestionStateMapProperty() {
        return ingestionStateMap;
    }
}
