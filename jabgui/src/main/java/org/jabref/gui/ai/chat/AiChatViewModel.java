package org.jabref.gui.ai.chat;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.ChatModelFactory;
import org.jabref.logic.ai.chatting.tasks.GenerateRagResponseTask;
import org.jabref.logic.ai.chatting.util.ChatHistoryUtils;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelFactory;
import org.jabref.logic.ai.ingestion.DocumentSplitterFactory;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.google.common.collect.Comparators;
import dev.langchain4j.data.segment.TextSegment;
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
    private final ListProperty<FullBibEntry> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<GenerateRagResponseTask> generateRagResponseTask = new SimpleObjectProperty<>();

    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private AsyncEmbeddingModel embeddingModel;
    private final ObjectProperty<DocumentSplitter> documentSplitter = new SimpleObjectProperty<>();

    private final TreeMap<List<FullBibEntry>, GenerateRagResponseTask> tasksMap =
            new TreeMap<>(Comparators.lexicographical(Comparator.comparing(id -> id.entry().getId())));

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final NotificationService notificationService;
    private final IngestionTaskAggregator ingestionTaskAggregator;
    private final IngestedDocumentsRepository ingestedDocumentsRepository;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final TaskExecutor taskExecutor;

    // Direct properties replacing AiChatLogic
    private final ListProperty<ChatMessage> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty systemMessageTemplate = new SimpleStringProperty();
    private final StringProperty userMessageTemplate = new SimpleStringProperty();

    public AiChatViewModel(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            IngestionTaskAggregator ingestionTaskAggregator,
            IngestedDocumentsRepository ingestedDocumentsRepository,
            NotificationService notificationService,
            EmbeddingStore<TextSegment> embeddingStore,
            TaskExecutor taskExecutor
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.notificationService = notificationService;
        this.ingestionTaskAggregator = ingestionTaskAggregator;
        this.ingestedDocumentsRepository = ingestedDocumentsRepository;
        this.embeddingStore = embeddingStore;
        this.taskExecutor = taskExecutor;

        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        systemMessageTemplate.bind(aiPreferences.chattingSystemMessageTemplateProperty());
        userMessageTemplate.bind(aiPreferences.chattingUserMessageTemplateProperty());

        BooleanBinding isAiTurnedOff = aiPreferences.enableAiProperty().not();
        BooleanBinding isWaiting = generateRagResponseTask.isNotNull();
        BooleanBinding hasNoFiles = Bindings.createBooleanBinding(() ->
                        entries.get() == null ||
                                entries.isEmpty() ||
                                entries.stream().flatMap(identifier -> identifier.entry().getFiles().stream()).findAny().isEmpty(),
                entries
        );

        BooleanBinding isError = Bindings.createBooleanBinding(() -> {
            if (chatHistory.isEmpty()) {
                return false;
            }
            return chatHistory.getLast().role() == ChatMessage.Role.ERROR;
        }, chatHistory);

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

        // Listen to system message template changes and update chat history
        BindingsHelper.subscribeToChanges(
                () -> ChatHistoryUtils.updateSystemMessage(chatHistory, systemMessageTemplate.get()),
                systemMessageTemplate
        );

        // Rebuild chat model when relevant preferences change (also calls immediately)
        BindingsHelper.subscribeToChanges(
                this::rebuildChatModel,
                aiPreferences.enableAiProperty(),
                aiPreferences.aiProviderProperty(),
                aiPreferences.customizeExpertSettingsProperty(),
                aiPreferences.temperatureProperty()
        );
        aiPreferences.addListenerToChatModels(this::rebuildChatModel);
        aiPreferences.addListenerToApiBaseUrls(this::rebuildChatModel);
        aiPreferences.setApiKeyChangeListener(this::rebuildChatModel);

        // Rebuild embedding model when relevant preferences change (also calls immediately)
        BindingsHelper.subscribeToChanges(
                this::rebuildEmbeddingModel,
                aiPreferences.enableAiProperty(),
                aiPreferences.customizeExpertSettingsProperty(),
                aiPreferences.embeddingModelProperty()
        );

        // Rebuild document splitter when relevant preferences change (also calls immediately)
        BindingsHelper.subscribeToChanges(
                this::rebuildDocumentSplitter,
                aiPreferences.customizeExpertSettingsProperty(),
                aiPreferences.documentSplitterKindProperty(),
                aiPreferences.documentSplitterChunkSizeProperty(),
                aiPreferences.documentSplitterOverlapSizeProperty()
        );
    }

    private void rebuildChatModel() {
        chatModel.set(ChatModelFactory.create(aiPreferences));
    }

    private void rebuildEmbeddingModel() {
        if (embeddingModel != null) {
            embeddingModel.close();
        }
        embeddingModel = EmbeddingModelFactory.create(aiPreferences, notificationService, taskExecutor);
    }

    private void rebuildDocumentSplitter() {
        documentSplitter.set(DocumentSplitterFactory.create(aiPreferences));
    }

    private void changeEmbeddingTasks() {
        generateEmbeddingsTasks.clear();
        // It's okay to pass null.
        generateRagResponseTask.set(tasksMap.get(entries));

        entries.forEach(identifier ->
                identifier.entry().getFiles().forEach(file -> {
                            GenerateEmbeddingsTask task = ingestionTaskAggregator.start(
                                    new GenerateEmbeddingsTaskRequest(
                                            filePreferences,
                                            ingestedDocumentsRepository,
                                            embeddingStore,
                                            embeddingModel,
                                            documentSplitter.get(),
                                            identifier.databaseContext(),
                                            file
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

        clearGenerateRagResponseTask();

        // Add user message to chat history
        ChatMessage userChatMessage = ChatMessage.userMessage(userMessage);
        chatHistory.add(userChatMessage);

        // Create the RAG task directly
        GenerateRagResponseTask task = new GenerateRagResponseTask(
                chatModel.get(),
                answerEngine.get(),
                List.copyOf(chatHistory), // Pass immutable copy
                userMessage,
                entries.get(),
                systemMessageTemplate.get(),
                userMessageTemplate.get()
        );

        List<FullBibEntry> taskEntries = entries.get();

        task.onSuccess(chatHistory::add);

        task.onFailure(ex -> chatHistory.add(ChatMessage.errorMessage(ex)));

        task.onFinished(() -> {
            tasksMap.remove(taskEntries);
            if (generateRagResponseTask.get() == task) {
                generateRagResponseTask.set(null);
            }
        });

        task.executeWith(taskExecutor);
        generateRagResponseTask.set(task);
        tasksMap.put(taskEntries, task);
    }

    private void clearGenerateRagResponseTask() {
        if (generateRagResponseTask.get() != null) {
            if (!generateRagResponseTask.get().isCancelled()) {
                generateRagResponseTask.get().cancel();
            }
            generateRagResponseTask.set(null);
        }
    }

    public void cancel() {
        assert state.get() == State.WAITING_FOR_MESSAGE || state.get() == State.ERROR;

        if (state.get() == State.WAITING_FOR_MESSAGE) {
            clearGenerateRagResponseTask();
        } else if (state.get() == State.ERROR) {
            if (!chatHistory.isEmpty()) {
                chatHistory.removeLast();
            }
        }
    }

    public void delete(String id) {
        assert state.get() == State.IDLE;
        ChatHistoryUtils.delete(chatHistory, id);
    }

    public void regenerate(String id) {
        assert state.get() == State.ERROR || state.get() == State.IDLE;

        String contentToRegenerate = ChatHistoryUtils.regenerate(chatHistory, id);

        if (contentToRegenerate != null) {
            sendMessage(contentToRegenerate);
        }
    }

    public void regenerate() {
        if (!chatHistory.isEmpty()) {
            regenerate(chatHistory.getLast().id());
        }
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return chatHistory;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return chatModel;
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return answerEngine;
    }

    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return generateEmbeddingsTasks;
    }
}
