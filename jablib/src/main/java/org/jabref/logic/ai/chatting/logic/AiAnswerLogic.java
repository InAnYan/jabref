package org.jabref.logic.ai.chatting.logic;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.ai.chatting.tasks.GenerateLlmResponseTask;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.RelevantInformation;

public class AiAnswerLogic {
    private final ChatModel chatModel;
    private final List<ChatMessage> chatHistory;
    private final AnswerEngine answerEngine;
    private final ChattingUserMessageAiTemplate injectionTemplate;

    public AiAnswerLogic(
            ChatModel chatModel,
            List<ChatMessage> chatHistory,
            AnswerEngine answerEngine,
            ChattingUserMessageAiTemplate injectionTemplate
    ) {
        this.chatModel = chatModel;
        this.chatHistory = chatHistory;
        this.answerEngine = answerEngine;
        this.injectionTemplate = injectionTemplate;
    }

    public GenerateLlmResponseTask answer(
            String userMessageContent,
            List<BibEntryAiIdentifier> entries
    ) {
        ChatMessage userMessage = ChatMessage.userMessage(userMessageContent);
        chatHistory.add(userMessage);

        List<RelevantInformation> relevantInformation = answerEngine.process(
                userMessage.content(),
                entries
        );

        String injected = injectionTemplate.render(
                entries.stream().map(BibEntryAiIdentifier::entry).toList(),
                userMessage.content(),
                relevantInformation
        );

        ChatMessage injectedMessage = ChatMessage.userMessage(userMessage.timestamp(), injected);

        List<ChatMessage> chatHistoryForLlm = new ArrayList<>(chatHistory);
        if (!chatHistoryForLlm.isEmpty()) {
            chatHistoryForLlm.removeLast();
        }
        chatHistoryForLlm.add(injectedMessage);

        return new GenerateLlmResponseTask(
                chatModel,
                chatHistoryForLlm,
                relevantInformation
        );
    }
}
