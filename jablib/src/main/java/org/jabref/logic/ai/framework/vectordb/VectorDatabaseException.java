package org.jabref.logic.ai.framework.vectordb;

import org.jabref.logic.l10n.Localization;

/**
 * Exception thrown when vector database operations fail.
 */
public class VectorDatabaseException extends Exception {

    @Override
    public String getLocalizedMessage() {
        return Localization.lang("An error occurred in the vector database.");
    }
}
