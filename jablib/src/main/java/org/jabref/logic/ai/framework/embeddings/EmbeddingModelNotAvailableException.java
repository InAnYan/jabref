package org.jabref.logic.ai.framework.embeddings;

import org.jabref.logic.l10n.Localization;

/**
 * Exception thrown when the embedding model is not available.
 */
public class EmbeddingModelNotAvailableException extends EmbeddingException {

    @Override
    public String getLocalizedMessage() {
        return Localization.lang("Embedding model is not available.");
    }
}
