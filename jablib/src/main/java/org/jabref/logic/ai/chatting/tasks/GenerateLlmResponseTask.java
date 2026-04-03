package org.jabref.logic.ai.chatting.tasks;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.pipeline.RelevantInformation;

import dev.langchain4j.model.chat.response.ChatResponse;

public class GenerateLlmResponseTask extends BackgroundTask<ChatMessage> {
    private final ChatModel chatModel;
    private final List<ChatMessage> chatHistory;
    private final List<RelevantInformation> relevantInformation;

    /// Relevant information is not added, but it's used to propagate to the resulting AI message.
    public GenerateLlmResponseTask(
            ChatModel chatModel,
            List<ChatMessage> chatHistory,
            List<RelevantInformation> relevantInformation
    ) {
        this.chatModel = chatModel;
        this.chatHistory = chatHistory;
        this.relevantInformation = relevantInformation;

        showToUser(true);
        titleProperty().set(Localization.lang("Waiting for AI reply..."));
    }

    @Override
    public ChatMessage call() throws Exception {
        List<dev.langchain4j.data.message.ChatMessage> chatMessages = chatHistory
                .stream()
                .map(ChatMessage::toLangChainMessage)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        ChatResponse response = chatModel.chat(chatMessages);
        String content = response.aiMessage().text();

        return ChatMessage.aiMessage(
                content,
                relevantInformation
        );
    }
}
