package org.jabref.logic.pipeline;

import java.util.Optional;

import org.jabref.logic.util.TaskExecutor;

/**
 * Pipeline that caches processing results to avoid redundant computations.
 * <p>
 * Extends the base pipeline with result caching functionality. If an object
 * has been processed before, returns the cached result immediately.
 */
public class CachedResultsPipeline<I extends Identifiable, O extends Identifiable> extends Pipeline<I, O> {
    /** Repository for storing and retrieving cached results. */
    private final ResultsRepository<O> resultsRepository;

    /**
     * Creates a cached pipeline with the given task executor and results repository.
     *
     * @param taskExecutor the task executor for background processing
     * @param resultsRepository the repository for caching results
     */
    public CachedResultsPipeline(TaskExecutor taskExecutor, ResultsRepository<O> resultsRepository) {
        super(taskExecutor);
        this.resultsRepository = resultsRepository;
    }

    /**
     * Stores the result in the repository when processing completes successfully.
     *
     * @param obj the original object that was processed
     * @param result the result of the processing
     */
    @Override
    protected void onResultSet(I obj, O result) {
        resultsRepository.storeResult(obj, result);
    }

    /**
     * Processes the object, checking cache first before delegating to parent.
     * <p>
     * If the object has been processed before, returns a completed tracker
     * with the cached result. Otherwise, delegates to the parent pipeline.
     *
     * @param obj the identifiable object to be processed
     * @return a tracker that may be immediately FINISHED if cached, or PROCESSING if not
     */
    @Override
    public PipelineObjectTracker<I, O> process(I obj) {
        if (resultsRepository.isProcessed(obj)) {
            Optional<O> result = resultsRepository.getResult(obj);
            if (result.isPresent()) {
                PipelineObjectTracker<I, O> tracker = new PipelineObjectTracker<>(obj);
                tracker.resultProperty().set(result.get());
                tracker.stateProperty().set(PipelineObjectTracker.State.FINISHED);
                return tracker;
            }
        }

        return super.process(obj);
    }
}
