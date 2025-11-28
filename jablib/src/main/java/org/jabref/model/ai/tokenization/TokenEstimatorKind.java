package org.jabref.model.ai.tokenization;

/// Idea taken from: <https://community.openai.com/t/what-is-the-openai-algorithm-to-calculate-tokens/58237/4>.
public enum TokenEstimatorKind {
    /// Average between (word count / 0.75) and (character count / 4).
    AVERAGE,

    /// 0.75 words = 1 token.
    WORDS,

    /// 4 characters = 1 token.
    CHARS,

    /// Maximum between (word count / 0.75) and (character count / 4).
    MAX,

    /// Minimum between (word count / 0.75) and (character count / 4).
    MIN
}
