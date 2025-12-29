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

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.logic.AiChatLogic;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.messages.ErrorMessage;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;

import dev.langchain4j.data.message.UserMessage;

public class AiChatViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        NO_FILES,
        IDLE,
        WAITING_FOR_MESSAGE,
        ERROR
    }

    private final GuiPreferences preferences;
    private final AiService aiService;
    private final TaskExecutor taskExecutor;

    private final AiChatLogic aiChatLogic;

    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();
    private final ListProperty<BibEntryAiIdentifier> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ChatHistoryRecordV2> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.IDLE);
    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ObjectProperty<BackgroundTask<ChatHistoryRecordV2>> generateLlmResponseTask = new SimpleObjectProperty<>();

    public AiChatViewModel(
            GuiPreferences preferences,
            AiService aiService,
            TaskExecutor taskExecutor
    ) {
        this.preferences = preferences;
        this.aiService = aiService;
        this.taskExecutor = taskExecutor;

        this.aiChatLogic = new AiChatLogic(
                aiService.getTemplatesFeature().getCurrentAiTemplates().getChattingUserMessageTemplate()
        );
        this.aiChatLogic.chatModelProperty().set(aiService.getChattingFeature().getCurrentChatModel());
        this.aiChatLogic.answerEngineProperty().bind(answerEngine);


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
            } else if (entries.get() == null || entries.isEmpty() || entries.get().stream().flatMap(identifier -> identifier.entry().getFiles().stream()).toList().isEmpty()) {
                state.set(State.NO_FILES);
            } else {
                changeEmbeddingTasks();
                state.set(State.IDLE);
            }
        });
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
                    clearGenerateLlmResponseTask();
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
            if (!generateLlmResponseTask.get().isCancelled()) {
                generateLlmResponseTask.get().cancel();
            }

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

        Optional<ChatHistoryRecordV2> recordOpt = chatHistory
                .stream()
                .filter(message ->
                        Objects.equals(message.id(), id))
                .findFirst();

        if (recordOpt.isEmpty()) {
            return;
        }

        ChatHistoryRecordV2 message = recordOpt.get();
        String contentToRegenerate = message.content();
        Instant cutoffTime = message.createdAt();

        if (!Objects.equals(message.messageTypeClassName(), UserMessage.class.getName())) {
            int index = chatHistory.indexOf(message);
            if (index > 0) {
                ChatHistoryRecordV2 prev = chatHistory.get(index - 1);
                if (Objects.equals(prev.messageTypeClassName(), UserMessage.class.getName())) {
                    contentToRegenerate = prev.content();
                    cutoffTime = prev.createdAt();
                }
            }
        }

        state.set(State.IDLE);

        final Instant finalCutoffTime = cutoffTime;
        chatHistory.removeIf(message2 ->
                !message2.createdAt().isBefore(finalCutoffTime)
        );

        sendMessage(contentToRegenerate);
    }

    public void regenerate() {
        if (!chatHistory.isEmpty()) {
            regenerate(chatHistory.getLast().id());
        }
    }

    public ListProperty<BibEntryAiIdentifier> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatHistoryRecordV2> chatHistoryProperty() {
        return chatHistory;
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
