package org.jabref.logic.ai.framework.embeddings;

import org.jabref.logic.l10n.Localization;

/**
 * Exception thrown when embedding computation fails.
 */
public class EmbeddingComputationException extends EmbeddingException {

    @Override
    public String getLocalizedMessage() {
        return Localization.lang("Failed to compute embeddings.");
    }
}
