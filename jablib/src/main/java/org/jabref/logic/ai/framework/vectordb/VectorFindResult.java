package org.jabref.logic.ai.framework.vectordb;

import java.util.Map;

/**
 * Result of a vector database similarity search operation.
 */
public class VectorFindResult {
    private final String text;
    private final double score;
    private final Map<String, Object> metadata;

    /**
     * Creates a new vector find result.
     *
     * @param text the text content of the found document
     * @param score the similarity score (typically 0.0 to 1.0)
     * @param metadata additional metadata associated with the document
     */
    public VectorFindResult(String text, double score, Map<String, Object> metadata) {
        this.text = text;
        this.score = score;
        this.metadata = Map.copyOf(metadata); // defensive copy
    }

    /**
     * Returns the text content of the found document.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the similarity score.
     *
     * @return the score
     */
    public double getScore() {
        return score;
    }

    /**
     * Returns the metadata associated with the document.
     *
     * @return the metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
