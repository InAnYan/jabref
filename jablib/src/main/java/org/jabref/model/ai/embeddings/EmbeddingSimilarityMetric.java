package org.jabref.model.ai.embeddings;

import org.jabref.logic.l10n.Localization;

public enum EmbeddingSimilarityMetric {
    COSINE_SIMILARITY;

    public String getDisplayName() {
        return switch (this) {
            case COSINE_SIMILARITY ->
                    Localization.lang("Cosine Similarity");
        };
    }
}
