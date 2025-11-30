package org.jabref.logic.ai.chatting.logic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.templates.ChattingSystemMessageAiTemplate;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.chatting.util.ChatHistory;
import org.jabref.logic.ai.chatting.util.ChatHistoryRecordUtils;
import org.jabref.logic.ai.pipeline.logic.rag.AnswerEngine;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.messages.ErrorMessage;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.ai.templating.AiTemplateKind;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatLogic {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatLogic.class);

    private final AiPreferences aiPreferences;
    private final ChatModel chatLanguageModel;
    private final ChattingSystemMessageAiTemplate chattingSystemMessageTemplate;
    private final ChattingUserMessageAiTemplate chattingUserMessageTemplate;

    private final ChatHistory chatHistory;
    private final ObservableList<BibEntry> entries;
    private final StringProperty name;
    private final BibDatabaseContext bibDatabaseContext;

    private final List<ChatMessage> chatMemory;

    private final AnswerEngine answerEngine;

    public AiChatLogic(
            AiPreferences aiPreferences,
            ChatModel chatLanguageModel,
            ChattingSystemMessageAiTemplate chattingSystemMessageTemplate,
            ChattingUserMessageAiTemplate chattingUserMessageTemplate,
            BibDatabaseContext bibDatabaseContext,
            ChatHistory chatHistory,
            ObservableList<BibEntry> entries,
            StringProperty name,
            AnswerEngine answerEngine
    ) {
        this.aiPreferences = aiPreferences;
        this.chatLanguageModel = chatLanguageModel;
        this.chattingSystemMessageTemplate = chattingSystemMessageTemplate;
        this.chattingUserMessageTemplate = chattingUserMessageTemplate;
        this.chatHistory = chatHistory;
        this.entries = entries;
        this.name = name;
        this.bibDatabaseContext = bibDatabaseContext;
        this.answerEngine = answerEngine;

        this.chatMemory = new ArrayList<>();
        chatHistory
                .getAllMessages()
                .stream()
                .filter(chatMessage -> !Objects.equals(chatMessage.messageTypeClassName(), ErrorMessage.class.getName()))
                .forEach(record -> chatMemory.add(ChatHistoryRecordUtils.toLangchainMessage(record)));
        setSystemMessage(chattingSystemMessageTemplate.render(entries));

        setupListeningToPreferencesChanges();
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences
                .templateProperty(AiTemplateKind.CHATTING_SYSTEM_MESSAGE)
                .addListener(obs ->
                        setSystemMessage(chattingSystemMessageTemplate.render(entries)));
    }

    private void setSystemMessage(String systemMessage) {
        if (chatMemory.isEmpty()) {
            chatMemory.add(new SystemMessage(systemMessage));
        } else {
            chatMemory.set(0, new SystemMessage(systemMessage));
        }
    }

    public AiMessage execute(UserMessage message) {
        chatHistory.addMessage(new ChatHistoryRecordV2(
                UUID.randomUUID().toString(),
                message.getClass().getName(),
                message.singleText(),
                Instant.now()
        ));

        LOGGER.info(
                "Sending message to AI provider ({}) for answering in {}: {}",
                aiPreferences.getAiProvider().getApiUrl(),
                name.get(),
                message.singleText()
        );

        List<RelevantInformation> excerpts = answerEngine.process(
                // TODO: think about this.
                new LongTaskInfo(new ProgressCounter(), new SimpleBooleanProperty(false)),
                message.singleText(),
                entries.stream().map(entry -> new FullBibEntryAiIdentifier(bibDatabaseContext, entry)).toList()
        );

        chatMemory.add(new UserMessage(chattingUserMessageTemplate.render(entries, message.singleText(), excerpts)));

        AiMessage aiMessage = chatLanguageModel.chat(chatMemory).aiMessage();

        chatMemory.set(chatMemory.size() - 1, message); // Removing excerpts from the chat history.
        chatMemory.add(aiMessage);
        chatHistory.addMessage(new ChatHistoryRecordV2(
                UUID.randomUUID().toString(),
                aiMessage.getClass().getName(),
                aiMessage.text(),
                Instant.now()
        ));

        LOGGER.debug("Message was answered by the AI provider for {}: {}", name.get(), aiMessage.text());

        return aiMessage;
    }
}
