package org.jabref.gui.ai.chat;

import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.rag.util.AnswerEngineFactory;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.entry.LinkedFile;

public class AiChatStatusViewModel extends AbstractViewModel {
    public enum FileStatus {
        PENDING,
        PROCESSING,
        ERROR_WHILE_PROCESSING,
        INGESTED,
        CANCELLED
    }

    public static class IngestionStatusRow {
        private final LinkedFile linkedFile;
        private final ReadOnlyStringWrapper name;
        private final ObjectProperty<FileStatus> status;
        private final ObjectProperty<Exception> error;

        public IngestionStatusRow(LinkedFile linkedFile) {
            this.linkedFile = linkedFile;
            this.name = new ReadOnlyStringWrapper(linkedFile.getLink());
            this.status = new SimpleObjectProperty<>(FileStatus.PENDING);
            this.error = new SimpleObjectProperty<>();
        }

        public ReadOnlyStringWrapper nameProperty() { return name; }
        public ObjectProperty<FileStatus> statusProperty() { return status; }
        public FileStatus getStatus() { return status.get(); }
        public ObjectProperty<Exception> errorProperty() { return error; }
        public Exception getError() { return error.get(); }
        public LinkedFile getLinkedFile() { return linkedFile; }
    }

    private final AnswerEngineFactory answerEngineFactory;

    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final Map<GenerateEmbeddingsTask, ChangeListener<? super TrackedBackgroundTask.Status>> taskListeners = new HashMap<>();

    private final ListProperty<FullBibEntryAiIdentifier> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObservableList<IngestionStatusRow> ingestionStatuses = FXCollections.observableArrayList(row ->
            new Observable[] {row.statusProperty(), row.errorProperty()}
    );

    private final ListProperty<AnswerEngineKind> answerEngineKinds = new SimpleListProperty<>(FXCollections.observableArrayList(AnswerEngineKind.values()));
    private final ObjectProperty<AnswerEngineKind> selectedAnswerEngineKind = new SimpleObjectProperty<>();
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();

    public AiChatStatusViewModel(GuiPreferences preferences, AiService aiService) {
        this.answerEngineFactory = new AnswerEngineFactory(
                preferences.getAiPreferences(),
                preferences.getFilePreferences(),
                aiService
        );

        this.selectedAnswerEngineKind.set(preferences.getAiPreferences().getAnswerEngineKind());
        this.selectedAnswerEngineKind.addListener((_, _, value) -> updateAnswerEngine(value));
        updateAnswerEngine(selectedAnswerEngineKind.get());

        setupTasksListeners();
    }

    private void setupTasksListeners() {
        generateEmbeddingsTasks.addListener((ListChangeListener<GenerateEmbeddingsTask>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    c.getRemoved().forEach(this::unwireTask);
                }
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(this::wireTask);
                }
            }
        });
    }

    private void wireTask(GenerateEmbeddingsTask task) {
        if (taskListeners.containsKey(task)) return;

        Platform.runLater(() -> getOrCreateRow(task.getLinkedFile()));

        ChangeListener<Object> statusListener = (_, _, _) -> processTask(task);
        taskListeners.put(task, statusListener);
        task.statusProperty().addListener(statusListener);

        processTask(task);
    }

    private void unwireTask(GenerateEmbeddingsTask task) {
        ChangeListener<? super TrackedBackgroundTask.Status> listener = taskListeners.remove(task);
        if (listener != null) {
            task.statusProperty().removeListener(listener);
        }
    }

    private IngestionStatusRow getOrCreateRow(LinkedFile file) {
        return ingestionStatuses.stream()
                                .filter(row -> row.getLinkedFile().equals(file))
                                .findFirst()
                                .orElseGet(() -> {
                                    IngestionStatusRow newRow = new IngestionStatusRow(file);
                                    ingestionStatuses.add(newRow);
                                    return newRow;
                                });
    }

    private void processTask(GenerateEmbeddingsTask task) {
        Platform.runLater(() -> {
            IngestionStatusRow row = getOrCreateRow(task.getLinkedFile());

            switch (task.getStatus()) {
                case SUCCESS -> {
                    row.statusProperty().set(FileStatus.INGESTED);
                    row.errorProperty().set(null);
                }
                case ERROR -> {
                    row.statusProperty().set(FileStatus.ERROR_WHILE_PROCESSING);
                    row.errorProperty().set(task.getException());
                }
                case PENDING -> row.statusProperty().set(FileStatus.PENDING);
                case PROCESSING -> row.statusProperty().set(FileStatus.PROCESSING);
                case CANCELLED -> row.statusProperty().set(FileStatus.CANCELLED);
            }
        });
    }

    private void updateAnswerEngine(AnswerEngineKind kind) {
        AnswerEngine engine = answerEngineFactory.create(kind);
        answerEngine.set(engine);
    }

    public ObservableList<IngestionStatusRow> getIngestionStatuses() {
        return ingestionStatuses;
    }

    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return generateEmbeddingsTasks;
    }

    public ListProperty<FullBibEntryAiIdentifier> entriesProperty() {
        return entries;
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return answerEngine;
    }

    public ListProperty<AnswerEngineKind> answerEngineKindsProperty() {
        return answerEngineKinds;
    }

    public ObjectProperty<AnswerEngineKind> selectedAnswerEngineKindProperty() {
        return selectedAnswerEngineKind;
    }
}
