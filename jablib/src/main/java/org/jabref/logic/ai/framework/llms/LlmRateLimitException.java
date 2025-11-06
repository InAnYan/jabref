package org.jabref.logic.ai.framework.llms;

import org.jabref.logic.l10n.Localization;

/**
 * Exception thrown when LLM service rate limit is exceeded.
 */
public class LlmRateLimitException extends LlmInferenceException {

    @Override
    public String getLocalizedMessage() {
        return Localization.lang("LLM service rate limit exceeded.");
    }
}
