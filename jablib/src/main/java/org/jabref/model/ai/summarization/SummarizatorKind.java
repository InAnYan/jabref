package org.jabref.model.ai.summarization;

import org.jabref.logic.l10n.Localization;

public enum SummarizatorKind {
    CHUNKED(Localization.lang("Chunked")),
    FULL_DOCUMENT(Localization.lang("Full Document"));

    private final String displayName;

    SummarizatorKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
