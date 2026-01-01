package org.jabref.logic.ai.chatting.logic;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.ai.chatting.tasks.GenerateLlmResponseTask;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.debug.AiDebugInformation;
import org.jabref.model.ai.debug.UserQueryStep;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.RelevantInformation;

public class AiAnswerLogic {
    private final ChatModel chatModel;
    private final List<ChatMessage> chatHistory;
    private final AnswerEngine answerEngine;
    private final ChattingUserMessageAiTemplate injectionTemplate;
    private final AiDebugInformation debugInformation;

    public AiAnswerLogic(
            ChatModel chatModel,
            List<ChatMessage> chatHistory,
            AnswerEngine answerEngine,
            ChattingUserMessageAiTemplate injectionTemplate,
            AiDebugInformation debugInformation
    ) {
        this.chatModel = chatModel;
        this.chatHistory = chatHistory;
        this.answerEngine = answerEngine;
        this.injectionTemplate = injectionTemplate;
        this.debugInformation = debugInformation;
    }

    public GenerateLlmResponseTask answer(
            String userMessageContent,
            List<BibEntryAiIdentifier> entries
    ) {
        ChatMessage userMessage = ChatMessage.userMessage(userMessageContent);
        chatHistory.add(userMessage);

        debugInformation.getSteps().add(UserQueryStep.now(userMessageContent));

        List<RelevantInformation> relevantInformation = answerEngine.process(
                LongTaskInfo.empty(),
                userMessage.getContent(),
                entries
        );

        String injected = injectionTemplate.render(
                entries.stream().map(BibEntryAiIdentifier::entry).toList(),
                userMessage.getContent(),
                relevantInformation
        );

        ChatMessage injectedMessage = ChatMessage.userMessage(userMessage.getTimestamp(), injected);

        List<ChatMessage> chatHistoryForLlm = new ArrayList<>(chatHistory);
        if (!chatHistoryForLlm.isEmpty()) {
            chatHistoryForLlm.removeLast();
        }
        chatHistoryForLlm.add(injectedMessage);

        return new GenerateLlmResponseTask(
                chatModel,
                chatHistoryForLlm,
                relevantInformation,
                debugInformation
        );
    }
}
