package org.jabref.logic.ai.framework.embeddings;

/**
 * Types of embeddings that can be generated for different use cases.
 */
public enum EmbeddingType {
    /**
     * Embedding optimized for question-like content.
     */
    QUESTION,

    /**
     * Embedding optimized for answer-like content.
     */
    ANSWER
}
