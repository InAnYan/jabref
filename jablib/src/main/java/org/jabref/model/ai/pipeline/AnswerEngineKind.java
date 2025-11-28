package org.jabref.model.ai.pipeline;

import org.jabref.logic.l10n.Localization;

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
}
