package org.jabref.logic.pipeline;

import java.util.Optional;

/**
 * Repository interface for storing and retrieving pipeline processing results.
 * <p>
 * Enables caching of expensive computations to avoid redundant processing.
 *
 * @param <O> the type of the processed output object
 */
public interface ResultsRepository<O extends Identifiable> {
    /**
     * Checks if an object has already been processed and stored.
     *
     * @param obj the object to check
     * @return true if the object has been processed, false otherwise
     */
    boolean isProcessed(Identifiable obj);

    /**
     * Retrieves the cached result for a processed object.
     *
     * @param obj the object whose result should be retrieved
     * @return the cached result, or empty if not processed
     */
    Optional<O> getResult(Identifiable obj);

    /**
     * Stores the result of a processed object for future retrieval.
     *
     * @param obj the object that was processed
     * @param result the result of the processing
     */
    void storeResult(Identifiable obj, O result);
}
