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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.ChatModelFactory;
import org.jabref.logic.ai.chatting.logic.AiChatLogic;
import org.jabref.logic.ai.chatting.tasks.GenerateLlmResponseTask;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelFactory;
import org.jabref.logic.ai.followup.tasks.GenerateFollowUpQuestions;
import org.jabref.logic.ai.ingestion.DocumentSplitterFactory;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.google.common.collect.Comparators;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatViewModel.class);

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
    private final ObjectProperty<GenerateLlmResponseTask> generateLlmResponseTask = new SimpleObjectProperty<>();
    private final ListProperty<String> followUpQuestions = new SimpleListProperty<>(FXCollections.observableArrayList());
    private BackgroundTask<List<String>> generateFollowUpQuestionsTask;

    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private AsyncEmbeddingModel embeddingModel;
    private final ObjectProperty<DocumentSplitter> documentSplitter = new SimpleObjectProperty<>();

    private final TreeMap<List<FullBibEntry>, GenerateLlmResponseTask> tasksMap =
            new TreeMap<>(Comparators.lexicographical(Comparator.comparing(id -> id.entry().getId())));

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final NotificationService notificationService;
    private final IngestionTaskAggregator ingestionTaskAggregator;
    private final IngestedDocumentsRepository ingestedDocumentsRepository;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final TaskExecutor taskExecutor;

    private final AiChatLogic aiChatLogic;

    public AiChatViewModel(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            String chattingSystemMessageTemplate,
            String chattingUserMessageTemplate,
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

        this.aiChatLogic = new AiChatLogic(
                chattingSystemMessageTemplate,
                chattingUserMessageTemplate
        );

        setupBindings();
        setupListeners();
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
            return aiChatLogic.chatHistoryProperty().getLast().role() == ChatMessage.Role.ERROR;
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
        aiChatLogic.chatModelProperty().set(chatModel.get());
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
        generateLlmResponseTask.set(tasksMap.get(entries));

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

        followUpQuestions.clear();
        clearGenerateLlmResponseTask();

        final String sentUserMessage = userMessage;

        GenerateLlmResponseTask task = aiChatLogic
                .call(
                        userMessage,
                        entries
                );

        ObservableList<ChatMessage> taskChatHistory = aiChatLogic.chatHistoryProperty().get();
        List<FullBibEntry> taskEntries = entries.get();

        task.onSuccess(aiMessage -> {
            taskChatHistory.add(aiMessage);
            if (aiPreferences.getGenerateFollowUpQuestions() && chatModel.get() != null) {
                scheduleFollowUpQuestionsGeneration(sentUserMessage, aiMessage.content());
            }
        });

        task.onFailure(ex -> taskChatHistory.add(ChatMessage.errorMessage(ex)));

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

    private void scheduleFollowUpQuestionsGeneration(String userMessage, String aiResponse) {
        ChatModel currentChatModel = chatModel.get();
        if (currentChatModel == null) {
            return;
        }

        if (generateFollowUpQuestionsTask != null && !generateFollowUpQuestionsTask.isCancelled()) {
            generateFollowUpQuestionsTask.cancel();
        }

        generateFollowUpQuestionsTask = new GenerateFollowUpQuestions(
                currentChatModel,
                aiPreferences,
                userMessage,
                aiResponse
        );

        generateFollowUpQuestionsTask
                .onSuccess(followUpQuestions::setAll)
                .onFailure(ex -> LOGGER.warn("Failed to generate follow-up questions", ex))
                .executeWith(taskExecutor);
    }

    public void sendFollowUpMessage(String question) {
        followUpQuestions.clear();
        sendMessage(question);
    }

    public void clearChatHistory() {
        aiChatLogic.chatHistoryProperty().get().clear();
        followUpQuestions.clear();
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
        followUpQuestions.clear();
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

    public ListProperty<FullBibEntry> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
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

    public ListProperty<String> followUpQuestionsProperty() {
        return followUpQuestions;
    }
}
