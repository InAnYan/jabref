package org.jabref.gui.ai.chat;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.logic.AiChatLogicV2;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.rag.util.AnswerEngineFactory;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.messages.ErrorMessage;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.AnswerEngineKind;

import dev.langchain4j.data.message.UserMessage;

public class AiChatViewModel {
    public enum State {
        AI_TURNED_OFF,
        NO_ENTRIES,
        IDLE,
        WAITING_FOR_MESSAGE,
        ERROR
    }

    private final GuiPreferences preferences;
    private final AiService aiService;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    private final AnswerEngineFactory answerEngineFactory;
    private final AiChatLogicV2 aiChatLogic;

    private final ListProperty<FullBibEntryAiIdentifier> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ChatHistoryRecordV2> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.IDLE);
    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ListProperty<AnswerEngineKind> answerEngineKinds =
            new SimpleListProperty<>(FXCollections.observableArrayList(AnswerEngineKind.values()));
    private final ObjectProperty<AnswerEngineKind> selectedAnswerEngineKind = new SimpleObjectProperty<>();

    private final ObjectProperty<BackgroundTask<ChatHistoryRecordV2>> generateLlmResponseTask = new SimpleObjectProperty<>();

    public AiChatViewModel(
            GuiPreferences preferences,
            AiService aiService,
            DialogService dialogService,
            TaskExecutor taskExecutor
    ) {
        this.preferences = preferences;
        this.aiService = aiService;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.answerEngineFactory = new AnswerEngineFactory(preferences.getAiPreferences(), preferences.getFilePreferences(), aiService);
        this.aiChatLogic = new AiChatLogicV2(aiService.getTemplatesFeature().getCurrentAiTemplates().getChattingUserMessageTemplate());
        this.aiChatLogic.chatModelProperty().set(aiService.getChattingFeature().getCurrentChatModel());
        this.selectedAnswerEngineKind.set(preferences.getAiPreferences().getAnswerEngineKind());
        this.selectedAnswerEngineKind.addListener(_ -> updateAnswerEngine());
        updateAnswerEngine();

        if (!preferences.getAiPreferences().getEnableAi()) {
            state.set(State.AI_TURNED_OFF);
        }

        preferences.getAiPreferences().enableAiProperty().addListener((_, _, value) -> {
            if (value && generateEmbeddingsTasks.isEmpty()) {
                changeEmbeddingTasks();
            }
        });

        this.entries.addListener((_, _, _) -> {
            if (!preferences.getAiPreferences().getEnableAi()) {
                state.set(State.AI_TURNED_OFF);
            } else if (entries.get() == null || entries.isEmpty()) {
                state.set(State.NO_ENTRIES);
            } else {
                changeEmbeddingTasks();
                state.set(State.IDLE);
            }
        });

        // Fixed: Use ListChangeListener to detect when items are added
        this.chatHistory.addListener((ListChangeListener<ChatHistoryRecordV2>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    clearGenerateLlmResponseTask();
                    state.set(State.IDLE);
                }
            }
        });
    }

    private void updateAnswerEngine() {
        AnswerEngine answerEngine = answerEngineFactory.create(selectedAnswerEngineKind.get());
        aiChatLogic.answerEngineProperty().set(answerEngine);
    }

    private void changeEmbeddingTasks() {
        generateEmbeddingsTasks.clear();

        if (!preferences.getAiPreferences().getEnableAi()) {
            return;
        }

        entries.forEach(identifier ->
                identifier.entry().getFiles().forEach(file -> {
                            GenerateEmbeddingsTask task = aiService.getIngestionFeature().getIngestionTaskAggregator().start(
                                    new GenerateEmbeddingsTaskRequest(
                                            preferences.getFilePreferences(),
                                            aiService.getIngestionFeature().getIngestedDocumentsRepository(),
                                            aiService.getIngestionFeature().getEmbeddingsStore(),
                                            aiService.getEmbeddingFeature().getCurrentEmbeddingModel(),
                                            aiService.getIngestionFeature().getCurrentDocumentSplitter(),
                                            identifier.databaseContext(),
                                            file,
                                            aiService.getShutdownSignal()
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

        state.set(State.WAITING_FOR_MESSAGE);

        clearGenerateLlmResponseTask();

        ChatHistoryRecordV2 userMessageRecord = new ChatHistoryRecordV2(
                UUID.randomUUID().toString(),
                UserMessage.class.getName(),
                userMessage,
                Instant.now()
        );
        chatHistory.add(userMessageRecord);

        BackgroundTask<ChatHistoryRecordV2> task = aiChatLogic
                .call(
                        userMessageRecord,
                        entries,
                        chatHistory
                )
                .onSuccess(message -> {
                    chatHistory.add(message);
                    state.set(State.IDLE);
                })
                .onFailure(ex -> {
                    chatHistory.add(new ChatHistoryRecordV2(
                            UUID.randomUUID().toString(),
                            ErrorMessage.class.getName(),
                            ex.getMessage(),
                            Instant.now())
                    );
                    state.set(State.ERROR);
                });

        task.executeWith(taskExecutor);

        generateLlmResponseTask.set(task);
    }

    private void clearGenerateLlmResponseTask() {
        if (generateLlmResponseTask.get() != null) {
            generateLlmResponseTask.get().cancel();
            generateLlmResponseTask.set(null);
        }
    }

    public void cancel() {
        assert state.get() == State.WAITING_FOR_MESSAGE || state.get() == State.ERROR;
        state.set(State.IDLE);

        if (state.get() == State.WAITING_FOR_MESSAGE) {
            clearGenerateLlmResponseTask();
        }

        if (state.get() == State.ERROR) {
            if (!chatHistory.isEmpty()) {
                chatHistory.removeLast();
            }
            state.set(State.IDLE);
        }
    }

    public void delete(String id) {
        assert state.get() == State.IDLE;

        chatHistory.removeIf(message -> Objects.equals(message.id(), id));
    }

    public void regenerate(String id) {
        assert state.get() == State.ERROR || state.get() == State.IDLE;

        state.set(State.WAITING_FOR_MESSAGE);

        Optional<ChatHistoryRecordV2> record = chatHistory
                .stream()
                .filter(message ->
                        Objects.equals(message.id(), id))
                .findFirst();

        record.ifPresent(message -> {
            chatHistory.removeIf(message2 ->
                    !message2.createdAt().isBefore(message.createdAt())
            );

            sendMessage(message.content());
        });
    }

    public void regenerate() {
        if (!chatHistory.isEmpty()) {
            regenerate(chatHistory.getLast().id());
        }
    }

    public void showIngestionStatus() {
        AiIngestionWindow window = new AiIngestionWindow(generateEmbeddingsTasks);
        dialogService.showCustomDialogAndWait(window);
    }

    public ListProperty<FullBibEntryAiIdentifier> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatHistoryRecordV2> chatHistoryProperty() {
        return chatHistory;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public ListProperty<AnswerEngineKind> answerEngineKindsProperty() {
        return answerEngineKinds;
    }

    public ObjectProperty<AnswerEngineKind> selectedAnswerEngineKindProperty() {
        return selectedAnswerEngineKind;
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return aiChatLogic.chatModelProperty();
    }
}
