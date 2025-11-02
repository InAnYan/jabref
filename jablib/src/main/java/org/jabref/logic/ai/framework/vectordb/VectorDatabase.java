package org.jabref.logic.ai.framework.vectordb;

import java.util.List;
import java.util.Map;

/**
 * Interface for vector database implementations.
 */
public interface VectorDatabase {

    /**
     * Adds a document with its embedding and metadata to the database.
     *
     * @param text the text content of the document
     * @param embedding the embedding vector for the text
     * @param metadata additional metadata to associate with the document
     * @throws VectorDatabaseException if the operation fails
     */
    void add(String text, List<Float> embedding, Map<String, Object> metadata) throws VectorDatabaseException;

    /**
     * Finds documents similar to the query embedding.
     *
     * @param queryEmbedding the embedding vector to search for
     * @param params parameters controlling the search (minimum score, max results)
     * @param filter metadata filter for narrowing search scope (empty map means no filter)
     * @return list of similar documents ordered by relevance
     * @throws VectorDatabaseException if the operation fails
     */
    List<VectorFindResult> find(List<Float> queryEmbedding, VectorDatabaseFindParameters params, Map<String, Object> filter) throws VectorDatabaseException;

    /**
     * Removes documents matching the filter criteria.
     *
     * @param filter metadata filter specifying which documents to remove
     * @throws VectorDatabaseException if the operation fails
     */
    void remove(Map<String, Object> filter) throws VectorDatabaseException;
}
