package org.jabref.model.ai.pipeline;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.AiDefaultEnums;

public enum AnswerEngineKind {
    EMBEDDINGS_SEARCH(Localization.lang("Embeddings Search")),
    FULL_DOCUMENT(Localization.lang("Full Document"));

    private final String displayName;

    AnswerEngineKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AnswerEngineKind safeValueOf(String name) {
        try {
            return AnswerEngineKind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AiDefaultEnums.ANSWER_ENGINE_KIND;
        }
    }
}
