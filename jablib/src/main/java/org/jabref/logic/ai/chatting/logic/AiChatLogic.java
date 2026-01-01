package org.jabref.logic.ai.chatting.logic;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.ai.chatting.tasks.GenerateLlmResponseTask;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.debug.AiDebugInformation;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;

public class AiChatLogic {
    private final ObjectProperty<ChattingUserMessageAiTemplate> template;

    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();
    private final ListProperty<ChatMessage> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    public AiChatLogic(ChattingUserMessageAiTemplate template) {
        this.template = new SimpleObjectProperty<>(template);
    }

    /**
     * Adds the user message to history, processes RAG, constructs the LLM context, and returns the generation task.
     */
    public GenerateLlmResponseTask call(
            String userMessageContent,
            List<BibEntryAiIdentifier> entries
    ) {
        Objects.requireNonNull(chatModel.get());
        Objects.requireNonNull(answerEngine.get());

        AiAnswerLogic aiAnswerLogic = new AiAnswerLogic(
                chatModel.get(),
                chatHistory,
                answerEngine.get(),
                template.get(),
                new AiDebugInformation()
        );

        return aiAnswerLogic.answer(
                userMessageContent,
                entries
        );
    }

    /**
     * Removes the message with the specified ID from history.
     * <p>
     * Leaves a "hole" in context, but this is intended.
     */
    public void delete(String id) {
        chatHistory.removeIf(message -> Objects.equals(message.getId(), id));
    }

    /**
     * Rewinds history to the point before the specified message and returns the user content to be re-sent.
     */
    public String regenerate(String id) {
        Optional<ChatMessage> recordOpt = chatHistory
                .stream()
                .filter(message -> Objects.equals(message.getId(), id))
                .findFirst();

        if (recordOpt.isEmpty()) {
            return null;
        }

        ChatMessage message = recordOpt.get();
        String contentToRegenerate = message.getContent();
        Instant cutoffTime = message.getTimestamp();

        if (message.getRole() != ChatMessage.Role.USER) {
            int index = chatHistory.indexOf(message);
            if (index > 0) {
                ChatMessage prev = chatHistory.get(index - 1);
                if (message.getRole() == ChatMessage.Role.USER) {
                    contentToRegenerate = prev.getContent();
                    cutoffTime = prev.getTimestamp();
                }
            }
        }

        final Instant finalCutoffTime = cutoffTime;
        chatHistory.removeIf(historyMessage ->
                !historyMessage.getTimestamp().isBefore(finalCutoffTime)
        );

        return contentToRegenerate;
    }

    public ObjectProperty<ChattingUserMessageAiTemplate> templateProperty() {
        return template;
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return chatModel;
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return answerEngine;
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return chatHistory;
    }
}
