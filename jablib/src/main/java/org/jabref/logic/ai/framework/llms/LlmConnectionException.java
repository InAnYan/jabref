package org.jabref.logic.ai.framework.llms;

import org.jabref.logic.l10n.Localization;

/**
 * Exception thrown when connection to LLM service fails.
 */
public class LlmConnectionException extends LlmInferenceException {

    @Override
    public String getLocalizedMessage() {
        return Localization.lang("Failed to connect to LLM service.");
    }
}
