package org.jabref.model.ai.pipeline;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.AiDefaultEnums;

public enum DocumentSplitterKind {
    SLIDING_WINDOW(Localization.lang("Sliding Window"));

    private final String displayName;

    DocumentSplitterKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DocumentSplitterKind safeValueOf(String name) {
        try {
            return DocumentSplitterKind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AiDefaultEnums.DOCUMENT_SPLITTER_KIND;
        }
    }
}
