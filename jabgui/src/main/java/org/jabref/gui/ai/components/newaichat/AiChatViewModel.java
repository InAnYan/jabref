package org.jabref.gui.ai.components.newaichat;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.util.ChatHistory;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.pipeline.logic.rag.AnswerEngine;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;

public class AiChatViewModel {
    public enum State {
        IDLE,
        WAITING_FOR_MESSAGE,
        ERROR
    }

    private final AiService aiService;

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.IDLE);
    private final ObjectProperty<ChatHistory> chatHistory = new SimpleObjectProperty<>();

    // IDLE properties.
    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ListProperty<ChatHistoryRecordV2> chatMessages = new SimpleListProperty<>();
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();

    // ERROR properties.
    private final ObjectProperty<Exception> exception = new SimpleObjectProperty<>();

    public AiChatViewModel(AiService aiService) {
        this.aiService = aiService;
    }

    public void bind(FullBibEntryAiIdentifier identifier) {
        chatMessages.clear();
        exception.set(null);

        aiService.getEntryChatHistoryRepository();

        state.set(State.IDLE);
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
}
