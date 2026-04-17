package org.jabref.model.ai.embeddings;

import org.jabref.logic.l10n.Localization;

public enum EmbeddingSimilarityMetric {
    COSINE_SIMILARITY(Localization.lang("Cosine Similarity"));

    private final String displayName;

    EmbeddingSimilarityMetric(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
