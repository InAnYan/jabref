package org.jabref.gui.ai.chat;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.tasks.GenerateLlmResponseTask;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;

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

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.IDLE);
    private final ListProperty<FullBibEntryAiIdentifier> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());

    // IDLE properties.
    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ListProperty<ChatHistoryRecordV2> chatMessages = new SimpleListProperty<>();
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();

    // ERROR properties.
    private final ObjectProperty<Exception> exception = new SimpleObjectProperty<>();

    // WAITING_FOR_MESSAGE properties.
    private final ObjectProperty<BackgroundTask<ChatHistoryRecordV2>> generateLlmResponseTask = new SimpleObjectProperty<>();

    public AiChatViewModel(
            GuiPreferences preferences,
            AiService aiService,
            DialogService dialogService
    ) {
        this.preferences = preferences;
        this.aiService = aiService;
        this.dialogService = dialogService;

        this.entries.addListener((InvalidationListener) _ -> changeEmbeddingTasks());

        if (!preferences.getAiPreferences().getEnableAi()) {
            state.set(State.AI_TURNED_OFF);
        }

        preferences.getAiPreferences().enableAiProperty().addListener((_, _, value) -> {
            if (value && generateEmbeddingsTasks.isEmpty()) {
                changeEmbeddingTasks();
            }
        });
    }

    private void changeEmbeddingTasks() {
        generateEmbeddingsTasks.clear();

        entries.forEach(identifier ->
                identifier.entry().getFiles().forEach(file -> {
                            if (preferences.getAiPreferences().getEnableAi()) {
                                generateEmbeddingsTasks.add(
                                        aiService.getIngestionFeature().getIngestionTaskAggregator().start(
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
                                        )
                                );
                            }
                        }
                )
        );
    }

    public void setEntries(List<FullBibEntryAiIdentifier> entries) {
        this.entries.setAll(entries);

        if (!preferences.getAiPreferences().getEnableAi()) {
            state.set(State.AI_TURNED_OFF);
        } else if (entries.isEmpty()) {
            state.set(State.NO_ENTRIES);
        } else {
            state.set(State.IDLE);
        }
    }

    public void setChatHistory(ObservableList<ChatHistoryRecordV2> chatHistory) {
        exception.set(null);
        clearGenerateLlmResponseTask();
        this.chatMessages.setAll(chatHistory);
        state.set(State.IDLE);
    }

    public void sendMessage(String userMessage) {
        assert state.get() == State.IDLE;

        if (StringUtil.isBlank(userMessage)) {
            return;
        }

        state.set(State.WAITING_FOR_MESSAGE);

        clearGenerateLlmResponseTask();

        chatMessages.add(new ChatHistoryRecordV2(
                UUID.randomUUID().toString(),
                UserMessage.class.getName(),
                userMessage,
                Instant.now()
        ));

        BackgroundTask<ChatHistoryRecordV2> task = new GenerateLlmResponseTask(
                chatModel.get(),
                chatMessages
        )
                .onSuccess(message -> {
                    chatMessages.add(message);
                    state.set(State.IDLE);
                })
                .onFailure(ex -> {
                    exception.set(ex);
                    state.set(State.ERROR);
                });

        generateLlmResponseTask.set(task);
    }

    private void clearGenerateLlmResponseTask() {
        if (generateLlmResponseTask.get() != null) {
            generateLlmResponseTask.get().cancel();
            generateLlmResponseTask.set(null);
        }
    }

    // In error mode will cancel the error.
    // In waiting mode will cancel the request.
    public void cancel() {
        assert state.get() == State.WAITING_FOR_MESSAGE || state.get() == State.ERROR;
        state.set(State.IDLE);

        if (state.get() == State.WAITING_FOR_MESSAGE) {
            clearGenerateLlmResponseTask();
        }

        if (state.get() == State.ERROR) {
            chatMessages.removeLast();
            state.set(State.IDLE);
        }
    }

    public void delete(String id) {
        assert state.get() == State.IDLE;

        chatMessages.removeIf(message -> Objects.equals(message.id(), id));
    }

    public void regenerate(String id) {
        assert state.get() == State.ERROR;
        state.set(State.WAITING_FOR_MESSAGE);

        Optional<ChatHistoryRecordV2> record = chatMessages
                .stream()
                .filter(message ->
                        Objects.equals(message.id(), id))
                .findFirst();

        record
                .ifPresent(message -> {
                    chatMessages.removeIf(message2 ->
                            !message2.createdAt().isBefore(message.createdAt())
                    );

                    sendMessage(message.content());
                });
    }

    public void showIngestionStatus() {
        AiIngestionWindow window = new AiIngestionWindow(generateEmbeddingsTasks);
        dialogService.showCustomDialog(window);
    }
}
