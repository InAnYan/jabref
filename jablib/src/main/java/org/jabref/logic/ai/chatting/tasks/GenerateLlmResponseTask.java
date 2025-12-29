package org.jabref.logic.ai.chatting.tasks;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jabref.logic.ai.chatting.util.ChatHistoryRecordUtils;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

public class GenerateLlmResponseTask extends BackgroundTask<ChatHistoryRecordV2> {
    private final ChatModel chatModel;
    private final List<ChatHistoryRecordV2> chatHistory;

    public GenerateLlmResponseTask(
            ChatModel chatModel,
            List<ChatHistoryRecordV2> chatHistory
    ) {
        this.chatModel = chatModel;
        this.chatHistory = chatHistory;

        showToUser(true);
        titleProperty().set(Localization.lang("Waiting for AI reply..."));
    }

    @Override
    public ChatHistoryRecordV2 call() throws Exception {
        List<ChatMessage> chatMessages = ChatHistoryRecordUtils.convertRecordsToLangchain(chatHistory);

        ChatResponse response = chatModel.chat(chatMessages);
        String context = response.aiMessage().text();

        return new ChatHistoryRecordV2(
                UUID.randomUUID().toString(),
                AiMessage.class.getName(),
                context,
                Instant.now()
        );
    }
}
