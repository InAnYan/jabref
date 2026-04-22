package org.jabref.model.ai.tokenization;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.AiDefaultEnums;

/// Idea taken from: <https://community.openai.com/t/what-is-the-openai-algorithm-to-calculate-tokens/58237/4>.
public enum TokenEstimatorKind {
    /// Average between [TokenEstimatorKind#WORDS] and [TokenEstimatorKind#CHARS].
    AVERAGE(Localization.lang("Average")),

    /// 0.75 words = 1 token.
    WORDS(Localization.lang("Words")),

    /// 4 characters = 1 token.
    CHARS(Localization.lang("Characters")),

    /// Maximum between [TokenEstimatorKind#WORDS] and [TokenEstimatorKind#CHARS].
    MAX(Localization.lang("Max")),

    /// Minimum between [TokenEstimatorKind#WORDS] and [TokenEstimatorKind#CHARS].
    MIN(Localization.lang("Min"));

    private final String displayName;

    TokenEstimatorKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TokenEstimatorKind safeValueOf(String name) {
        try {
            return TokenEstimatorKind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AiDefaultEnums.TOKEN_ESTIMATOR_KIND;
        }
    }
}
