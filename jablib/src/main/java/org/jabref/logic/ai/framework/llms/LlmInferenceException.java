package org.jabref.logic.ai.framework.llms;

import org.jabref.logic.l10n.Localization;

/**
 * Exception thrown when LLM inference fails.
 */
public class LlmInferenceException extends Exception {

    @Override
    public String getLocalizedMessage() {
        return Localization.lang("An error occurred during LLM inference.");
    }
}
