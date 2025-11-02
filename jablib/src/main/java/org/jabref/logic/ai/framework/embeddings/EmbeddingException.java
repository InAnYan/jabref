package org.jabref.logic.ai.framework.embeddings;

import org.jabref.logic.l10n.Localization;

/**
 * Exception thrown when embedding operations fail.
 */
public class EmbeddingException extends Exception {

    @Override
    public String getLocalizedMessage() {
        return Localization.lang("An error occurred during embedding computation.");
    }
}
