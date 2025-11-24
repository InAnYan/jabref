package org.jabref.logic.ai.preferences;

import java.util.Map;

import org.jabref.model.ai.chatting.AiProvider;

public class AiProviderDefaultChatModels {
    private static final Map<AiProvider, PredefinedChatModel> CHAT_MODELS = Map.of(
            AiProvider.OPEN_AI, PredefinedChatModel.GPT_4O_MINI,
            AiProvider.MISTRAL_AI, PredefinedChatModel.OPEN_MIXTRAL_8X22B,
            AiProvider.GEMINI, PredefinedChatModel.GEMINI_1_5_FLASH,
            AiProvider.HUGGING_FACE, PredefinedChatModel.BLANK_HUGGING_FACE,
            AiProvider.GPT4ALL, PredefinedChatModel.BLANK_GPT4ALL
    );

    public static PredefinedChatModel getDefaultChatModel(AiProvider aiProvider) {
        return CHAT_MODELS.get(aiProvider);
    }
}
