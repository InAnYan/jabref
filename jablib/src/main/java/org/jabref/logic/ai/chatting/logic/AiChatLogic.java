package org.jabref.logic.ai.chatting.logic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.ai.chatting.tasks.GenerateLlmResponseTask;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.RelevantInformation;

import dev.langchain4j.data.message.UserMessage;

public class AiChatLogic {
    private final ObjectProperty<ChattingUserMessageAiTemplate> template;

    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();
    private final ListProperty<ChatHistoryRecordV2> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

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

        ChatHistoryRecordV2 userMessageRecord = new ChatHistoryRecordV2(
                UUID.randomUUID().toString(),
                UserMessage.class.getName(),
                userMessageContent,
                Instant.now()
        );

        chatHistory.add(userMessageRecord);

        List<RelevantInformation> relevantInformation = answerEngine.get().process(
                LongTaskInfo.empty(),
                userMessageRecord.content(),
                entries
        );

        String injected = template.get().render(
                entries.stream().map(BibEntryAiIdentifier::entry).toList(),
                userMessageRecord.content(),
                relevantInformation
        );

        ChatHistoryRecordV2 injectedMessage = new ChatHistoryRecordV2(
                UUID.randomUUID().toString(),
                UserMessage.class.getName(),
                injected,
                userMessageRecord.createdAt()
        );

        ArrayList<ChatHistoryRecordV2> chatHistoryForLlm = new ArrayList<>(chatHistory);
        if (!chatHistoryForLlm.isEmpty()) {
            chatHistoryForLlm.removeLast();
        }
        chatHistoryForLlm.add(injectedMessage);

        return new GenerateLlmResponseTask(chatModel.get(), chatHistoryForLlm);
    }

    /**
     * Removes the message with the specified ID from history.
     * <p>
     * Leaves a "hole" in context, but this is intended.
     */
    public void delete(String id) {
        chatHistory.removeIf(message -> Objects.equals(message.id(), id));
    }

    /**
     * Rewinds history to the point before the specified message and returns the user content to be re-sent.
     */
    public String regenerate(String id) {
        Optional<ChatHistoryRecordV2> recordOpt = chatHistory
                .stream()
                .filter(message -> Objects.equals(message.id(), id))
                .findFirst();

        if (recordOpt.isEmpty()) {
            return null;
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

        final Instant finalCutoffTime = cutoffTime;
        chatHistory.removeIf(historyMessage ->
                !historyMessage.createdAt().isBefore(finalCutoffTime)
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

    public ListProperty<ChatHistoryRecordV2> chatHistoryProperty() {
        return chatHistory;
    }
}
