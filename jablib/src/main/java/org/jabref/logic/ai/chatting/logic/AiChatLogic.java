package org.jabref.logic.ai.chatting.logic;

import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.templates.ChattingSystemMessageTemplate;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageTemplate;
import org.jabref.logic.ai.pipeline.logic.rag.AnswerEngine;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.ai.chatting.ErrorMessage;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.ai.templating.AiTemplate;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatLogic {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatLogic.class);

    private final AiPreferences aiPreferences;
    private final ChatModel chatLanguageModel;
    private final ChattingSystemMessageTemplate chattingSystemMessageTemplate;
    private final ChattingUserMessageTemplate chattingUserMessageTemplate;

    private final ObservableList<ChatMessage> chatHistory;
    private final ObservableList<BibEntry> entries;
    private final StringProperty name;
    private final BibDatabaseContext bibDatabaseContext;

    private ChatMemory chatMemory;

    private final AnswerEngine answerEngine;

    public AiChatLogic(
            AiPreferences aiPreferences,
            ChatModel chatLanguageModel,
            ChattingSystemMessageTemplate chattingSystemMessageTemplate,
            ChattingUserMessageTemplate chattingUserMessageTemplate,
            BibDatabaseContext bibDatabaseContext,
            ObservableList<ChatMessage> chatHistory,
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

        setupListeningToPreferencesChanges();
        rebuildFull(chatHistory);
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences
                .templateProperty(AiTemplate.CHATTING_SYSTEM_MESSAGE)
                .addListener(obs ->
                        setSystemMessage(chattingSystemMessageTemplate.render(entries)));

        aiPreferences.contextWindowSizeProperty().addListener(obs -> rebuildFull(chatMemory.messages()));
    }

    private void rebuildFull(List<ChatMessage> chatMessages) {
        rebuildChatMemory(chatMessages);
    }

    private void rebuildChatMemory(List<ChatMessage> chatMessages) {
        // TODO: remove this. No algorithm for squashing the conversation.
        // Because we can't get a tokenizer for each model, {@link AiChatLogic} assumes that
        // every text is tokenized like it's tokenized for OpenAI's GPT-4o-mini model.
        //
        // Reasons why we can't get tokenizer for each model:
        // - Some tokenizers might not be available in langchain4j.
        // - User may use a custom model, but there is no way to supply a custom tokenizer.
        // - OpenAI API (and compatible ones) doesn't have an endpoint for tokenizing text.
        //
        // This is another dark workaround of AI integration. But it works "good-enough" for now.
        this.chatMemory = TokenWindowChatMemory
                .builder()
                .maxTokens(aiPreferences.getContextWindowSize(), new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4_O_MINI))
                .build();

        chatMessages.stream().filter(chatMessage -> !(chatMessage instanceof ErrorMessage)).forEach(chatMemory::add);

        setSystemMessage(chattingSystemMessageTemplate.render(entries));
    }

    private void setSystemMessage(String systemMessage) {
        chatMemory.add(new SystemMessage(systemMessage));
    }

    public AiMessage execute(UserMessage message) {
        // Message will be automatically added to ChatMemory through ConversationalRetrievalChain.

        chatHistory.add(message);

        LOGGER.info("Sending message to AI provider ({}) for answering in {}: {}",
                aiPreferences.getAiProvider().getApiUrl(),
                name.get(),
                message.singleText());


        // This is crazy, but langchain4j {@link ChatMemory} does not allow to remove single messages.
        ChatMemory tempChatMemory = TokenWindowChatMemory
                .builder()
                .maxTokens(aiPreferences.getContextWindowSize(), new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4_O_MINI))
                .build();

        chatMemory.messages().forEach(tempChatMemory::add);

        List<RelevantInformation> excerpts = answerEngine.process(
                // TODO: think about this.
                new LongTaskInfo(new ProgressCounter(), new SimpleBooleanProperty(false)),
                message.singleText(),
                entries.stream().map(entry -> new FullBibEntryAiIdentifier(bibDatabaseContext, entry)).toList()
        );

        tempChatMemory.add(new UserMessage(chattingUserMessageTemplate.render(entries, message.singleText(), excerpts)));
        chatMemory.add(message);

        AiMessage aiMessage = chatLanguageModel.chat(tempChatMemory.messages()).aiMessage();

        chatMemory.add(aiMessage);
        chatHistory.add(aiMessage);

        LOGGER.debug("Message was answered by the AI provider for {}: {}", name.get(), aiMessage.text());

        return aiMessage;
    }

    public ObservableList<ChatMessage> getChatHistory() {
        return chatHistory;
    }
}
