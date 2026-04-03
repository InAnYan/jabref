package org.jabref.logic.ai.chatting.logic;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.tasks.GenerateLlmResponseTask;
import org.jabref.logic.ai.chatting.templates.ChattingSystemMessageAiTemplate;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.RelevantInformation;

public class AiAnswerLogic {
    private final ChatModel chatModel;
    private final List<ChatMessage> chatHistory;
    private final AnswerEngine answerEngine;
    private final ChattingSystemMessageAiTemplate systemMessageTemplate;
    private final ChattingUserMessageAiTemplate injectionTemplate;

    public AiAnswerLogic(
            ChatModel chatModel,
            List<ChatMessage> chatHistory,
            AnswerEngine answerEngine,
            ChattingSystemMessageAiTemplate systemMessageTemplate,
            ChattingUserMessageAiTemplate injectionTemplate
    ) {
        this.chatModel = chatModel;
        this.chatHistory = chatHistory;
        this.answerEngine = answerEngine;
        this.systemMessageTemplate = systemMessageTemplate;
        this.injectionTemplate = injectionTemplate;
    }

    public GenerateLlmResponseTask answer(
            String userMessageContent,
            List<FullBibEntry> entries
    ) {
        ChatMessage userMessage = ChatMessage.userMessage(userMessageContent);
        chatHistory.add(userMessage);

        List<RelevantInformation> relevantInformation = answerEngine.process(
                userMessage.content(),
                entries
        );

        String injected = injectionTemplate.render(
                entries.stream().map(FullBibEntry::entry).toList(),
                userMessage.content(),
                relevantInformation
        );

        ChatMessage injectedMessage = ChatMessage.userMessage(userMessage.timestamp(), injected);

        ChatMessage systemMessage = ChatMessage.systemMessage(systemMessageTemplate.render(
                entries.stream().map(FullBibEntry::entry).toList()
        ));
        List<ChatMessage> chatHistoryForLlm = new ArrayList<>();
        chatHistoryForLlm.add(systemMessage);
        chatHistoryForLlm.addAll(chatHistory);

        // Removing plain user message and replacing it with augmented one from the answer engine.
        if (chatHistoryForLlm.getLast().role() != ChatMessage.Role.SYSTEM) {
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
