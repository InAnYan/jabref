package org.jabref.gui.ai.chat;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.logic.AiChatLogic;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.templates.AiTemplatesFactory;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.ErrorMessage;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;

import com.google.common.collect.Comparators;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class AiChatViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        NO_FILES,
        IDLE,
        WAITING_FOR_MESSAGE,
        ERROR
    }

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.IDLE);
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();
    private final ListProperty<BibEntryAiIdentifier> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<BackgroundTask<ChatHistoryRecordV2>> generateLlmResponseTask = new SimpleObjectProperty<>();

    private final TreeMap<List<BibEntryAiIdentifier>, BackgroundTask<ChatHistoryRecordV2>> tasksMap =
            new TreeMap<>(Comparators.lexicographical(Comparator.comparing(id -> id.entry().getId())));

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final ChatModel chatModel;
    private final IngestionTaskAggregator ingestionTaskAggregator;
    private final IngestedDocumentsRepository ingestedDocumentsRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentSplitter documentSplitter;
    private final ReadOnlyBooleanProperty shutdownSignal;
    private final TaskExecutor taskExecutor;

    private final AiChatLogic aiChatLogic;

    public AiChatViewModel(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            ChatModel chatModel,
            AiTemplatesFactory aiTemplatesFactory,
            IngestionTaskAggregator ingestionTaskAggregator,
            IngestedDocumentsRepository ingestedDocumentsRepository,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            DocumentSplitter documentSplitter,
            ReadOnlyBooleanProperty shutdownSignal,
            TaskExecutor taskExecutor
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.chatModel = chatModel;
        this.ingestionTaskAggregator = ingestionTaskAggregator;
        this.ingestedDocumentsRepository = ingestedDocumentsRepository;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.documentSplitter = documentSplitter;
        this.shutdownSignal = shutdownSignal;
        this.taskExecutor = taskExecutor;

        this.aiChatLogic = new AiChatLogic(
                aiTemplatesFactory.getChattingUserMessageTemplate()
        );

        setupBindings();
        setupListeners();
        setupValues();
    }

    private void setupBindings() {
        aiChatLogic.answerEngineProperty().bind(answerEngine);

        BooleanBinding isAiTurnedOff = aiPreferences.enableAiProperty().not();
        BooleanBinding isWaiting = generateLlmResponseTask.isNotNull();
        BooleanBinding hasNoFiles = Bindings.createBooleanBinding(() ->
                        entries.get() == null ||
                                entries.isEmpty() ||
                                entries.stream().flatMap(identifier -> identifier.entry().getFiles().stream()).findAny().isEmpty(),
                entries
        );

        BooleanBinding isError = Bindings.createBooleanBinding(() -> {
            if (aiChatLogic.chatHistoryProperty().isEmpty()) {
                return false;
            }
            return ErrorMessage.class.getName().equals(aiChatLogic.chatHistoryProperty().getLast().messageTypeClassName());
        }, aiChatLogic.chatHistoryProperty());

        BindingsHelper.bindEnum(
                state,
                State.AI_TURNED_OFF, isAiTurnedOff,
                State.WAITING_FOR_MESSAGE, isWaiting,
                State.NO_FILES, hasNoFiles,
                State.ERROR, isError,
                State.IDLE
        );
    }

    private void setupListeners() {
        BooleanBinding entriesPresent = entries.isNotNull().and(entries.emptyProperty().not());

        ListenersHelper.runWhenListChangesWithPrecondition(
                entries,
                aiPreferences.enableAiProperty().and(entriesPresent),
                this::changeEmbeddingTasks
        );
    }

    private void setupValues() {
        aiChatLogic.chatModelProperty().set(chatModel);
    }

    private void changeEmbeddingTasks() {
        generateEmbeddingsTasks.clear();
        // It's okay to pass null.
        generateLlmResponseTask.set(tasksMap.get(entries));

        entries.forEach(identifier ->
                identifier.entry().getFiles().forEach(file -> {
                            GenerateEmbeddingsTask task = ingestionTaskAggregator.start(
                                    new GenerateEmbeddingsTaskRequest(
                                            filePreferences,
                                            ingestedDocumentsRepository,
                                            embeddingStore,
                                            embeddingModel,
                                            documentSplitter,
                                            identifier.databaseContext(),
                                            file,
                                            shutdownSignal
                                    )
                            );

                            generateEmbeddingsTasks.add(task);
                        }
                )
        );
    }

    public void sendMessage(String userMessage) {
        assert state.get() == State.IDLE;

        if (StringUtil.isBlank(userMessage)) {
            return;
        }

        clearGenerateLlmResponseTask();

        BackgroundTask<ChatHistoryRecordV2> task = aiChatLogic
                .call(
                        userMessage,
                        entries
                );

        ObservableList<ChatHistoryRecordV2> taskChatHistory = aiChatLogic.chatHistoryProperty().get();
        List<BibEntryAiIdentifier> taskEntries = entries.get();

        task.onSuccess(a ->
                taskChatHistory.add(a));

        task.onFailure(ex ->
                taskChatHistory.add(new ChatHistoryRecordV2(
                        UUID.randomUUID().toString(),
                        ErrorMessage.class.getName(),
                        ex.getMessage(),
                        Instant.now())
                ));

        task.onFinished(() -> {
            tasksMap.remove(taskEntries);
            if (generateLlmResponseTask.get() == task) {
                generateLlmResponseTask.set(null);
            }
        });

        task.executeWith(taskExecutor);
        generateLlmResponseTask.set(task);
        tasksMap.put(taskEntries, task);
    }

    private void clearGenerateLlmResponseTask() {
        if (generateLlmResponseTask.get() != null) {
            if (!generateLlmResponseTask.get().isCancelled()) {
                generateLlmResponseTask.get().cancel();
            }
            generateLlmResponseTask.set(null);
        }
    }

    public void cancel() {
        assert state.get() == State.WAITING_FOR_MESSAGE || state.get() == State.ERROR;

        if (state.get() == State.WAITING_FOR_MESSAGE) {
            clearGenerateLlmResponseTask();
        } else if (state.get() == State.ERROR) {
            if (!aiChatLogic.chatHistoryProperty().isEmpty()) {
                aiChatLogic.chatHistoryProperty().removeLast();
            }
        }
    }

    public void delete(String id) {
        assert state.get() == State.IDLE;
        aiChatLogic.delete(id);
    }

    public void regenerate(String id) {
        assert state.get() == State.ERROR || state.get() == State.IDLE;

        String contentToRegenerate = aiChatLogic.regenerate(id);

        if (contentToRegenerate != null) {
            sendMessage(contentToRegenerate);
        }
    }

    public void regenerate() {
        if (!aiChatLogic.chatHistoryProperty().isEmpty()) {
            regenerate(aiChatLogic.chatHistoryProperty().getLast().id());
        }
    }

    public ListProperty<BibEntryAiIdentifier> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatHistoryRecordV2> chatHistoryProperty() {
        return aiChatLogic.chatHistoryProperty();
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return aiChatLogic.chatModelProperty();
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return answerEngine;
    }

    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return generateEmbeddingsTasks;
    }
}
