package org.jabref.logic.ai.framework.vectordb;

import org.jabref.logic.l10n.Localization;

/**
 * Exception thrown when vector database query operations fail.
 */
public class VectorDatabaseQueryException extends VectorDatabaseException {

    @Override
    public String getLocalizedMessage() {
        return Localization.lang("Vector database query failed.");
    }
}
