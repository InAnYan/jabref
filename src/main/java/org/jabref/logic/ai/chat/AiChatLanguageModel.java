package org.jabref.logic.ai.chat;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.ai.chathistory.BibDatabaseChatHistory;
import org.jabref.preferences.AiPreferences;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.h2.mvstore.MVStore;

/**
 * Wrapper around langchain4j chat language model.
 * <p>
 * This class listens to preferences changes.
 */
public class AiChatLanguageModel implements ChatLanguageModel {
    private final AiPreferences aiPreferences;

    private Optional<OpenAiService> openAiService = Optional.empty();

    public AiChatLanguageModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        if (aiPreferences.getEnableChatWithFiles()) {
            rebuild();
        }

        setupListeningToPreferencesChanges();
    }

    /**
     * Update the underlying {@link ChatLanguageModel} by current {@link AiPreferences} parameters.
     * When the model is updated, the chat messages are not lost.
     * See {@link AiChatLogic}, where messages are stored in {@link ChatMemory},
     * and {@link BibDatabaseChatHistory}, where messages are stored in {@link MVStore}.
     */
    private void rebuild() {
        if (!aiPreferences.getEnableChatWithFiles() || aiPreferences.getOpenAiToken().isEmpty()) {
            openAiService = Optional.empty();
            return;
        }

        openAiService = Optional.of(new OpenAiService(aiPreferences.getOpenAiToken()));

        /*
        ChatLanguageModel chatLanguageModel =
                OpenAiChatModel
                        .builder()
                        .apiKey(aiPreferences.getOpenAiToken())
                        .modelName(aiPreferences.getChatModel().getLabel())
                        .temperature(aiPreferences.getTemperature())
                        .logRequests(true)
                        .logResponses(true)
                        .build();
         */
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.enableChatWithFilesProperty().addListener(obs -> rebuild());
        aiPreferences.openAiTokenProperty().addListener(obs -> rebuild());
        aiPreferences.chatModelProperty().addListener(obs -> rebuild());
        aiPreferences.temperatureProperty().addListener(obs -> rebuild());
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> list) {
        if (openAiService.isEmpty()) {
            return new Response<>(new AiMessage("Error: chatting is disallowed or you provided an empty token"), new TokenUsage(), FinishReason.OTHER);
        }

        List<com.theokanning.openai.completion.chat.ChatMessage> convertedMessages =
                list.stream().map(langchainMessage -> new com.theokanning.openai.completion.chat.ChatMessage(langchainMessage.type() == ChatMessageType.AI ? ChatMessageRole.ASSISTANT.value() : (langchainMessage.type() == ChatMessageType.SYSTEM ? ChatMessageRole.SYSTEM.value() : ChatMessageRole.USER.value()), langchainMessage.text()))
                        .toList();

        ChatCompletionRequest request = ChatCompletionRequest
                .builder()
                .model(aiPreferences.getChatModel().getLabel())
                .temperature(aiPreferences.getTemperature())
                .n(1)
                .messages(convertedMessages)
                .build();

        ChatCompletionResult result = openAiService.get().createChatCompletion(request);
        List<ChatCompletionChoice> choices = result.getChoices();

        if (choices.isEmpty()) {
            return new Response<>(new AiMessage("Error: OpenAI returned no messages"), new TokenUsage(), FinishReason.OTHER);
        } else {
            return new Response<>(new AiMessage(choices.getFirst().getMessage().getContent()));
        }
    }
}
