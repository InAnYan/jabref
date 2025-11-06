package org.jabref.logic.ai.framework.embeddings;

import java.util.List;

/**
 * Interface for embedding model implementations.
 */
public interface EmbeddingModel {

    /**
     * Computes an embedding vector for the given text.
     *
     * @param text the text to embed
     * @param type the type of embedding to generate
     * @return the embedding vector as a list of floats
     * @throws EmbeddingException if embedding computation fails
     */
    List<Float> compute(String text, EmbeddingType type) throws EmbeddingException;
}
