package org.jabref.model.ai.pipeline;

import org.jabref.logic.l10n.Localization;

public enum DocumentSplitterKind {
    SLIDING_WINDOW(Localization.lang("Sliding Window"));

    private final String displayName;

    DocumentSplitterKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
