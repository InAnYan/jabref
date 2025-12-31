package org.jabref.gui.ai.chat;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.rag.util.AnswerEngineFactory;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class AiChatStatusViewModel extends AbstractViewModel {
    public enum FileStatus {
        PENDING,
        PROCESSING,
        ERROR_WHILE_PROCESSING,
        INGESTED,
        CANCELLED;

        public String getDisplayName() {
            return switch (this) {
                case PENDING ->
                        "Pending";
                case PROCESSING ->
                        "Processing";
                case ERROR_WHILE_PROCESSING ->
                        "Error";
                case INGESTED ->
                        "Ingested";
                case CANCELLED ->
                        "Cancelled";
            };
        }
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

        public ReadOnlyStringWrapper nameProperty() {
            return name;
        }

        public ObjectProperty<FileStatus> statusProperty() {
            return status;
        }

        public FileStatus getStatus() {
            return status.get();
        }

        public ObjectProperty<Exception> errorProperty() {
            return error;
        }

        public Exception getError() {
            return error.get();
        }

        public LinkedFile getLinkedFile() {
            return linkedFile;
        }
    }

    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final Map<GenerateEmbeddingsTask, ChangeListener<? super TrackedBackgroundTask.Status>> taskListeners = new HashMap<>();

    private final ListProperty<BibEntryAiIdentifier> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObservableList<IngestionStatusRow> ingestionStatuses = FXCollections.observableArrayList(row ->
            new Observable[] {row.statusProperty(), row.errorProperty()}
    );

    private final ListProperty<AnswerEngineKind> answerEngineKinds = new SimpleListProperty<>(FXCollections.observableArrayList(AnswerEngineKind.values()));
    private final ObjectProperty<AnswerEngineKind> selectedAnswerEngineKind = new SimpleObjectProperty<>();
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();

    private final AiPreferences aiPreferences;
    private final AnswerEngineFactory answerEngineFactory;

    public AiChatStatusViewModel(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        this.aiPreferences = aiPreferences;
        this.answerEngineFactory = new AnswerEngineFactory(
                aiPreferences,
                filePreferences,
                embeddingModel,
                embeddingStore
        );

        setupListeners();
        setupValues();
    }

    private void setupListeners() {
        ListenersHelper.onChangeNonNull(selectedAnswerEngineKind, this::updateAnswerEngine);
        ListenersHelper.onListContentsChange(generateEmbeddingsTasks, this::wireTask, this::unwireTask);
    }

    private void setupValues() {
        selectedAnswerEngineKind.set(aiPreferences.getAnswerEngineKind());
    }

    private void wireTask(GenerateEmbeddingsTask task) {
        if (taskListeners.containsKey(task))
            return;

        UiTaskExecutor.runInJavaFXThread(() -> getOrCreateRow(task.getLinkedFile()));

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

        UiTaskExecutor.runInJavaFXThread(() ->
                ingestionStatuses.removeIf(row -> row.getLinkedFile().equals(task.getLinkedFile()))
        );
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
        UiTaskExecutor.runInJavaFXThread(() -> {
            IngestionStatusRow row = getOrCreateRow(task.getLinkedFile());

            switch (task.getStatus()) {
                case SUCCESS -> {
                    row.errorProperty().set(null);
                    row.statusProperty().set(FileStatus.INGESTED);
                }
                case ERROR -> {
                    row.errorProperty().set(task.getException());
                    row.statusProperty().set(FileStatus.ERROR_WHILE_PROCESSING);
                }
                case PENDING ->
                        row.statusProperty().set(FileStatus.PENDING);
                case PROCESSING ->
                        row.statusProperty().set(FileStatus.PROCESSING);
                case CANCELLED ->
                        row.statusProperty().set(FileStatus.CANCELLED);
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

    public ListProperty<BibEntryAiIdentifier> entriesProperty() {
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
