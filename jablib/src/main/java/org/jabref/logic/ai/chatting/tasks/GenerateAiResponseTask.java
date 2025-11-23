package org.jabref.logic.ai.chatting.tasks;

import org.jabref.logic.ai.chatting.algorithms.AiChatLogic;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

public class GenerateAiResponseTask extends BackgroundTask<AiMessage> {
    private final UserMessage userMessage;
    private final AiChatLogic aiChatLogic;

    public GenerateAiResponseTask(UserMessage userMessage, AiChatLogic aiChatLogic) {
        this.userMessage = userMessage;
        this.aiChatLogic = aiChatLogic;

        showToUser(true);
        titleProperty().set(Localization.lang("Waiting for AI reply..."));
    }

    @Override
    public AiMessage call() throws Exception {
        return aiChatLogic.execute(userMessage);
    }
}
