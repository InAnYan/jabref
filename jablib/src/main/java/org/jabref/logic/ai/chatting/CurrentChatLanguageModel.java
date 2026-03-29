package org.jabref.logic.ai.chatting;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.customimplementations.llms.JvmOpenAiChatLanguageModel;
import org.jabref.logic.ai.customimplementations.tokenization.algorithms.TokenEstimator;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.llm.AiProvider;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import jakarta.annotation.Nullable;

/**
 * Wrapper around langchain4j chat language model.
 * <p>
 * Notice, that the real chat model is created lazily, when it's needed. This is done, so API key is fetched only,
 * when user wants to chat with AI.
 */
public class CurrentChatLanguageModel implements ChatModel, AutoCloseable {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);

    private final AiPreferences aiPreferences;

    private final CurrentTokenEstimator currentTokenEstimator;

    private final HttpClient httpClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("ai-api-connection-pool-%d").build()
    );

    @Nullable
    private dev.langchain4j.model.chat.ChatModel langchainChatModel = null;

    public CurrentChatLanguageModel(
            AiPreferences aiPreferences,
            CurrentTokenEstimator currentTokenEstimator
    ) {
        this.aiPreferences = aiPreferences;

        this.currentTokenEstimator = currentTokenEstimator;

        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECTION_TIMEOUT).executor(executorService).build();

        setupListeningToPreferencesChanges();
    }

    private void rebuild() {
        String apiKey = aiPreferences.getApiKeyForAiProvider(aiPreferences.getAiProvider());
        if (!aiPreferences.getEnableAi() || apiKey.isEmpty()) {
            langchainChatModel = null;
            return;
        }

        switch (aiPreferences.getAiProvider()) {
            case OPEN_AI ->
                    langchainChatModel = new JvmOpenAiChatLanguageModel(aiPreferences, httpClient);

            case MISTRAL_AI ->
                    langchainChatModel = MistralAiChatModel
                            .builder()
                            .apiKey(apiKey)
                            .modelName(aiPreferences.getSelectedChatModel())
                            .temperature(aiPreferences.getTemperature())
                            .baseUrl(aiPreferences.getSelectedApiBaseUrl())
                            .logRequests(true)
                            .logResponses(true)
                            .build();

            case GEMINI -> // NOTE: {@link GoogleAiGeminiChatModel} doesn't support API base url.
                    langchainChatModel = GoogleAiGeminiChatModel
                            .builder()
                            .apiKey(apiKey)
                            .modelName(aiPreferences.getSelectedChatModel())
                            .temperature(aiPreferences.getTemperature())
                            .logRequestsAndResponses(true)
                            .build();

            case HUGGING_FACE -> // NOTE: {@link HuggingFaceChatModel} doesn't support API base url.
                    langchainChatModel = HuggingFaceChatModel
                            .builder()
                            .accessToken(apiKey)
                            .modelId(aiPreferences.getSelectedChatModel())
                            .temperature(aiPreferences.getTemperature())
                            .timeout(Duration.ofMinutes(2))
                            .build();
        }
    }

    private void setupListeningToPreferencesChanges() {
        // TODO: written below is weird, but works.
        // Setting "langchainChatModel" to "null" will trigger a rebuild on the next usage

        aiPreferences.enableAiProperty().addListener(_ -> langchainChatModel = null);
        aiPreferences.aiProviderProperty().addListener(_ -> langchainChatModel = null);
        aiPreferences.customizeExpertSettingsProperty().addListener(_ -> langchainChatModel = null);
        aiPreferences.temperatureProperty().addListener(_ -> langchainChatModel = null);

        aiPreferences.addListenerToChatModels(() -> langchainChatModel = null);
        aiPreferences.addListenerToApiBaseUrls(() -> langchainChatModel = null);
        aiPreferences.setApiKeyChangeListener(() -> langchainChatModel = null);
    }

    @Override
    public ChatResponse chat(List<ChatMessage> list) {
        // The rationale for RuntimeExceptions in this method:
        // 1. langchain4j error handling is a mess, and it uses RuntimeExceptions
        //    everywhere. Because this method implements a langchain4j interface,
        //    we follow the same "practice".
        // 2. There is no way to encode error information from type system: nor
        //    in the result type, nor "throws" in method signature. Actually,
        //    it's possible, but langchain4j doesn't do it.

        if (langchainChatModel == null) {
            if (!aiPreferences.getEnableAi()) {
                throw new RuntimeException(Localization.lang("In order to use AI chat, you need to enable chatting with attached PDF files in JabRef preferences (AI tab)."));
            } else if (aiPreferences.getApiKeyForAiProvider(aiPreferences.getAiProvider()).isEmpty()) {
                throw new RuntimeException(Localization.lang("In order to use AI chat, set an API key inside JabRef preferences (AI tab)."));
            } else {
                rebuild();
                if (langchainChatModel == null) {
                    throw new RuntimeException(Localization.lang("Unable to chat with AI."));
                }
            }
        }

        return langchainChatModel.chat(list);
    }

    @Override
    public void close() {
        httpClient.shutdownNow();
        executorService.shutdownNow();
    }

    @Override
    public TokenEstimator getTokenizer() {
        return currentTokenEstimator;
    }

    @Override
    public AiProvider getAiProvider() {
        return aiPreferences.getAiProvider();
    }

    @Override
    public String getName() {
        return aiPreferences.getSelectedChatModel();
    }

    @Override
    public int getContextWindowSize() {
        return aiPreferences.getContextWindowSize();
    }
}
