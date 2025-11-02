package org.jabref.logic.ai.framework.llms;

import org.jabref.logic.l10n.Localization;

/**
 * Exception thrown when LLM authentication fails.
 */
public class LlmAuthenticationException extends LlmInferenceException {

    @Override
    public String getLocalizedMessage() {
        return Localization.lang("LLM authentication failed.");
    }
}
