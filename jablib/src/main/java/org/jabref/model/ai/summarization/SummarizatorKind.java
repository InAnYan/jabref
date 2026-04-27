package org.jabref.model.ai.summarization;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.AiDefaultEnums;

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

    public static SummarizatorKind safeValueOf(String name) {
        try {
            return SummarizatorKind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AiDefaultEnums.SUMMARIZATOR_KIND;
        }
    }
}
