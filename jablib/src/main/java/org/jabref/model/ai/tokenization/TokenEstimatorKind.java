package org.jabref.model.ai.tokenization;

/// Idea taken from: <https://community.openai.com/t/what-is-the-openai-algorithm-to-calculate-tokens/58237/4>.
public enum TokenEstimatorKind {
    /// Average between [TokenEstimatorKind#WORDS] and [TokenEstimatorKind#CHARS].
    AVERAGE,

    /// 0.75 words = 1 token.
    WORDS,

    /// 4 characters = 1 token.
    CHARS,

    /// Maximum between [TokenEstimatorKind#WORDS] and [TokenEstimatorKind#CHARS].
    MAX,

    /// Minimum between [TokenEstimatorKind#WORDS] and [TokenEstimatorKind#CHARS].
    MIN
}
