package org.jabref.logic.ai.chatting.tasks;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.debug.AiDebugInformation;
import org.jabref.model.ai.debug.StepRecorder;
import org.jabref.model.ai.pipeline.RelevantInformation;

import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Nullable;

public class GenerateLlmResponseTask extends BackgroundTask<ChatMessage> {
    private final ChatModel chatModel;
    private final List<ChatMessage> chatHistory;
    private final List<RelevantInformation> relevantInformation;
    private final AiDebugInformation debugInformation;

    /// Relevant information is not added, but it's used to propagate to the resulting AI message.
    public GenerateLlmResponseTask(
            ChatModel chatModel,
            List<ChatMessage> chatHistory,
            List<RelevantInformation> relevantInformation,
            AiDebugInformation debugInformation
    ) {
        this.chatModel = chatModel;
        this.chatHistory = chatHistory;
        this.relevantInformation = relevantInformation;
        this.debugInformation = debugInformation;

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

        StepRecorder stepRecorder = new StepRecorder();

        try {
            ChatResponse response = chatModel.chat(chatMessages);
            String content = response.aiMessage().text();
            @Nullable String thinking = response.aiMessage().thinking();

            return ChatMessage.aiMessage(
                    content,
                    relevantInformation,
                    thinking,
                    debugInformation
            );
        } finally {
            debugInformation.getSteps().add(chatModel.fillDebugStep(stepRecorder));
        }
    }

    public AiDebugInformation getDebugInformation() {
        return debugInformation;
    }
}
