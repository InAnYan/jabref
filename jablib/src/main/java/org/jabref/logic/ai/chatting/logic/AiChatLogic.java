package org.jabref.logic.ai.chatting.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.ai.chatting.tasks.GenerateLlmResponseTask;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.RelevantInformation;

import dev.langchain4j.data.message.UserMessage;

public class AiChatLogic {
    private final ChattingUserMessageAiTemplate template;

    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();

    public AiChatLogic(ChattingUserMessageAiTemplate template) {
        this.template = template;
    }

    public GenerateLlmResponseTask call(
            ChatHistoryRecordV2 userMessage,
            List<FullBibEntryAiIdentifier> entries,
            List<ChatHistoryRecordV2> chatHistory
    ) {
        // In the app chat history, only raw AI response and user messages are saved.
        // However, to the AI, the latest user message is processed by the answer engine.
        // The processed message is sent to AI but not saved in the app chat history.
        // But:
        // The other code should add a raw user message to the chat history (this will allow
        // displaying the user message while we are waiting for the AI response). This
        // code will create AI chat history by deleting the last message (which is expected to
        // be a user message) and then adding the injected message. App chat history is left
        // intact.
        // Then add yourself the generated AI message to the app chat history.

        Objects.requireNonNull(chatModel.get());
        Objects.requireNonNull(answerEngine.get());
        Objects.requireNonNull(chatHistory);

        List<RelevantInformation> relevantInformation = answerEngine.get().process(
                LongTaskInfo.empty(),
                userMessage.content(),
                entries
        );

        String injected = template.render(
                entries.stream().map(FullBibEntryAiIdentifier::entry).toList(),
                userMessage.content(),
                relevantInformation
        );

        ChatHistoryRecordV2 injectedMessage = new ChatHistoryRecordV2(
                UUID.randomUUID().toString(),
                UserMessage.class.getName(),
                injected,
                userMessage.createdAt()
        );

        ArrayList<ChatHistoryRecordV2> chatHistoryWithInjectedMessage = new ArrayList<>(chatHistory);
        chatHistoryWithInjectedMessage.removeLast();
        chatHistoryWithInjectedMessage.add(injectedMessage);

        return new GenerateLlmResponseTask(chatModel.get(), chatHistoryWithInjectedMessage);
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return chatModel;
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return answerEngine;
    }
}
