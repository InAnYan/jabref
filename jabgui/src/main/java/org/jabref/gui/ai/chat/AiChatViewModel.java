package org.jabref.gui.ai.chat;

import java.util.List;

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
import org.jabref.logic.ai.chatting.tasks.GenerateAiResponseTask;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;

public class AiChatViewModel {
    public enum State {
        IDLE,
        WAITING_FOR_MESSAGE,
        ERROR
    }

    private final GuiPreferences preferences;
    private final AiService aiService;
    private final DialogService dialogService;

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.IDLE);
    private final ListProperty<BibEntryAiIdentifier> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());

    // IDLE properties.
    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ListProperty<ChatHistoryRecordV2> chatMessages = new SimpleListProperty<>();
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();

    // ERROR properties.
    private final ObjectProperty<Exception> exception = new SimpleObjectProperty<>();

    // WAITING_FOR_MESSAGE properties.
    private final ObjectProperty<GenerateAiResponseTask> generateAiResponseTask = new SimpleObjectProperty<>();

    public AiChatViewModel(
            GuiPreferences preferences,
            AiService aiService,
            DialogService dialogService
    ) {
        this.preferences = preferences;
        this.aiService = aiService;
        this.dialogService = dialogService;

        this.entries.addListener((InvalidationListener) _ -> changeEmbeddingTasks());
    }

    private void changeEmbeddingTasks() {
        generateEmbeddingsTasks.clear();

        entries.forEach(identifier ->
                generateEmbeddingsTasks.add(
                        aiService.getIngestionFeature().getIngestionTaskAggregator().start(
                                new GenerateEmbeddingsTaskRequest(
                                    preferences.getFilePreferences(),
                                        aiService.getIngestionFeature().getIngestedDocumentsRepository(),
                                        aiService.getIngestionFeature().getEmbeddingsStore(),
                                        aiService.getEmbeddingFeature().getCurrentEmbeddingModel(),
                                        aiService.getIngestionFeature().getCurrentDocumentSplitter(),
                                        aiService.
                                )
                        )
                ));
    }

    public void setEntries(List<FullBibEntryAiIdentifier> entries) {
        // remove old ones?
        // start ingestion
    }

    public void setChatHistory(ObservableList<ChatHistoryRecordV2> chatHistory) {
        // clear exception
        // clear chat history
        // set chat history
        // set state
    }

    public void sendMessage(String message) {
        assert state.get() == State.IDLE;
        state.set(State.WAITING_FOR_MESSAGE);
        // Send.
    }

    public void cancelRequest() {
        assert state.get() == State.WAITING_FOR_MESSAGE;
        state.set(State.IDLE);
        // Cancel task.
    }

    public void delete(String id) {
        assert state.get() == State.IDLE;
        // Find message.
        // Delete in UI.
        // Delete in repository.
    }

    public void regenerate(String id) {
        assert state.get() == State.ERROR;
        state.set(State.WAITING_FOR_MESSAGE);
        // Find message.
        // Delete all messages after.
        // Call AI.
    }

    public void cancel() {
        assert state.get() == State.ERROR;
        // Delete error message.
        state.set(State.IDLE);
    }

    public void showIngestionStatus() {
        AiIngestionWindow window = new AiIngestionWindow(generateEmbeddingsTasks);
        dialogService.showCustomDialog(window);
    }
}
