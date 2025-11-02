package org.jabref.logic.ai.framework.vectordb;

import org.jabref.logic.l10n.Localization;

/**
 * Exception thrown when connection to vector database fails.
 */
public class VectorDatabaseConnectionException extends VectorDatabaseException {

    @Override
    public String getLocalizedMessage() {
        return Localization.lang("Failed to connect to vector database.");
    }
}
