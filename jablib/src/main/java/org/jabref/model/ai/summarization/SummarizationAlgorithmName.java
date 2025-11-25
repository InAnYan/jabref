package org.jabref.model.ai.summarization;

import org.jabref.logic.l10n.Localization;

public enum SummarizationAlgorithmName {
    CHUNKED(Localization.lang("Chunked Summarization"));

    private final String displayName;

    SummarizationAlgorithmName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
