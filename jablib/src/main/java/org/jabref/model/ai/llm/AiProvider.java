package org.jabref.model.ai.llm;

import java.io.Serializable;

import org.jabref.logic.l10n.Localization;

public enum AiProvider implements Serializable {
    OPEN_AI(Localization.lang("OpenAI (or API compatible)"), "https://api.openai.com/v1", "https://openai.com/policies/privacy-policy/"),
    MISTRAL_AI(Localization.lang("Mistral AI"), "https://api.mistral.ai/v1", "https://mistral.ai/terms/#privacy-policy"),
    GEMINI(Localization.lang("Gemini"), "https://generativelanguage.googleapis.com/v1beta/", "https://ai.google.dev/gemini-api/terms"),
    HUGGING_FACE(Localization.lang("Hugging Face"), "https://huggingface.co/api", "https://huggingface.co/privacy"),
    GPT4ALL(Localization.lang("GPT4All"), "http://localhost:4891/v1", "https://www.nomic.ai/gpt4all/legal/privacy-policy");

    private final String displayName;
    private final String apiUrl;
    private final String privacyPolicyUrl;

    AiProvider(String displayName, String apiUrl, String privacyPolicyUrl) {
        this.displayName = displayName;
        this.apiUrl = apiUrl;
        this.privacyPolicyUrl = privacyPolicyUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getPrivacyPolicyUrl() {
        return privacyPolicyUrl;
    }

    public String toString() {
        return displayName;
    }
}

