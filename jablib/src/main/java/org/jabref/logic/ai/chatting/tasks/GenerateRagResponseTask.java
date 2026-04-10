package org.jabref.logic.ai.chatting.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.templates.AiTemplateRenderer;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.RelevantInformation;

import dev.langchain4j.model.chat.response.ChatResponse;

public class GenerateRagResponseTask extends BackgroundTask<ChatMessage> {
    private final ChatModel chatModel;
    private final AnswerEngine answerEngine;
    private final List<ChatMessage> chatHistory;
    private final String userMessageContent;
    private final List<FullBibEntry> entries;
    private final String systemMessageTemplate;
    private final String injectionTemplate;

    /**
     * Creates a task that processes RAG and generates an LLM response.
     * The input chat history is not modified; a new list is created internally.
     */
    public GenerateRagResponseTask(
            ChatModel chatModel,
            AnswerEngine answerEngine,
            List<ChatMessage> chatHistory,
            String userMessageContent,
            List<FullBibEntry> entries,
            String systemMessageTemplate,
            String injectionTemplate
    ) {
        this.chatModel = chatModel;
        this.answerEngine = answerEngine;
        this.chatHistory = chatHistory;
        this.userMessageContent = userMessageContent;
        this.entries = entries;
        this.systemMessageTemplate = systemMessageTemplate;
        this.injectionTemplate = injectionTemplate;

        showToUser(true);
        titleProperty().set(Localization.lang("Waiting for AI reply..."));
    }

    @Override
    public ChatMessage call() throws Exception {
        List<ChatMessage> workingChatHistory = new ArrayList<>(chatHistory);

        ChatMessage userMessage = null;
        for (int i = workingChatHistory.size() - 1; i >= 0; i--) {
            if (workingChatHistory.get(i).role() == ChatMessage.Role.USER) {
                userMessage = workingChatHistory.get(i);
                break;
            }
        }

        if (userMessage == null) {
            userMessage = ChatMessage.userMessage(userMessageContent);
            workingChatHistory.add(userMessage);
        }

        List<RelevantInformation> relevantInformation = answerEngine.process(
                userMessage.content(),
                entries
        );

        String injected = AiTemplateRenderer.renderChattingUserMessage(
                injectionTemplate,
                entries.stream().map(FullBibEntry::entry).toList(),
                userMessage.content(),
                relevantInformation
        );

        ChatMessage injectedMessage = ChatMessage.userMessage(userMessage.timestamp(), injected);

        ChatMessage systemMessage = ChatMessage.systemMessage(AiTemplateRenderer.renderChattingSystemMessage(
                systemMessageTemplate,
                entries.stream().map(FullBibEntry::entry).toList()
        ));

        List<ChatMessage> chatHistoryForLlm = new ArrayList<>();
        chatHistoryForLlm.add(systemMessage);
        chatHistoryForLlm.addAll(workingChatHistory);

        if (chatHistoryForLlm.getLast().role() != ChatMessage.Role.SYSTEM) {
            chatHistoryForLlm.removeLast();
        }
        chatHistoryForLlm.add(injectedMessage);

        List<dev.langchain4j.data.message.ChatMessage> chatMessages = chatHistoryForLlm
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




