package org.jabref.logic.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.logic.util.TaskExecutor;

/**
 * Manages a sequence of processing stages for identifiable objects.
 * <p>
 * Processes objects asynchronously using JabRef's task executor, providing tracking
 * and idempotency guarantees. Validates type compatibility between stages at runtime.
 */
public class Pipeline<I extends Identifiable, O extends Identifiable> {
    /** Ordered list of processing stages to execute. */
    private final List<PipelineStage> stages;
    /** Task executor for asynchronous processing. */
    private final TaskExecutor taskExecutor;

    /** Map to track objects by their ID for idempotency. */
    private final ConcurrentHashMap<String, PipelineObjectTracker<I, O>> trackerMap = new ConcurrentHashMap<>();

    /**
     * Creates a new pipeline with the given task executor for asynchronous processing.
     *
     * @param taskExecutor the task executor to use for background processing
     */
    public Pipeline(TaskExecutor taskExecutor) {
        this.stages = new ArrayList<>();
        this.taskExecutor = taskExecutor;
    }

    /**
     * Called when a result is set on the tracker.
     * Subclasses can override this to perform additional actions when processing completes successfully.
     *
     * @param obj the original object that was processed
     * @param result the result of the processing
     */
    protected void onResultSet(I obj, O result) {
        // Default implementation does nothing
    }

    /**
     * Returns the title to display for the background task.
     * Subclasses can override this to provide custom titles.
     *
     * @param obj the object being processed
     * @param currentStageInfo the info string of the currently executing stage
     * @return the title for the background task
     */
    protected String getBackgroundTaskTitle(I obj, String currentStageInfo) {
        return Localization.lang("Processing %0: %1", obj.getId(), currentStageInfo);
    }

    /**
     * Adds a pipeline stage to the list.
     * <p>
     * If the return type of the last stage in this pipeline does not equal
     * to the accepting type of the argument stage, a runtime exception is thrown.
     *
     * @param stage the pipeline stage to be added
     * @throws IllegalArgumentException if the return type of the last stage doesn't match the input type of the new stage
     */
    public void addStage(PipelineStage stage) {
        if (!stages.isEmpty()) {
            PipelineStage lastStage = stages.get(stages.size() - 1);
            Class<?> lastStageOutputType = PipelineStageTypeUtil.getOutputType(lastStage);
            Class<?> newStageInputType = PipelineStageTypeUtil.getInputType(stage);

            // Check if the last stage's output type can be assigned to the new stage's input type
            // This allows for subtyping: if last stage returns SubType and new stage accepts SuperType, that's OK
            if (!newStageInputType.isAssignableFrom(lastStageOutputType)) {
                throw new IllegalArgumentException(
                        String.format("Type mismatch: Last stage returns %s, but new stage accepts %s",
                                lastStageOutputType.getName(), newStageInputType.getName()));
            }
        }
        stages.add(stage);
    }

    /**
     * Processes the given identifiable object through all stages in the pipeline asynchronously.
     * <p>
     * Returns immediately with a tracker in the PROCESSING state. The actual processing
     * happens in the background using the provided executor.
     * <p>
     * Implements idempotency: if the same object (by ID) is sent while it's still processing,
     * the same tracker is returned. Once processing completes (FINISHED or ERROR), sending
     * the same object again creates a new tracker, allowing reprocessing.
     *
     * @param obj the identifiable object to be processed
     * @return a tracker that starts in the PROCESSING state and will be updated to FINISHED or ERROR
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PipelineObjectTracker<I, O> process(I obj) {
        String objId = obj.getId();

        // Check if there's an existing tracker for this object
        PipelineObjectTracker<I, O> existingTracker = trackerMap.get(objId);

        // If tracker exists and is still processing, return it (idempotency)
        if (existingTracker != null && existingTracker.isProcessing()) {
            return existingTracker;
        }

        // Create a new tracker and store it in the map
        PipelineObjectTracker<I, O> tracker = new PipelineObjectTracker<>(obj);
        trackerMap.put(objId, tracker);

        // If tracker exists but is complete (FINISHED or ERROR), or doesn't exist,
        // create a new tracker and start processing
        BackgroundTask<O> backgroundTask;
        try {
            backgroundTask = new PipelineBackgroundTask<>(
                obj, stages, this::getBackgroundTaskTitle, this::onResultSet);
        } catch (Exception e) {
            // Fallback for test environments where JavaFX toolkit might not be initialized
            backgroundTask = BackgroundTask.wrap(() -> {
                Identifiable result = obj;
                for (PipelineStage stage : stages) {
                    result = stage.process(result);
                }
                return (O) result;
            });
        }

        backgroundTask.onSuccess(res -> {
            tracker.resultProperty().set(res);
            tracker.stateProperty().set(PipelineObjectTracker.State.FINISHED);
            onResultSet(obj, res);
            trackerMap.remove(objId);
        }).onFailure(ex -> {
            tracker.errorProperty().set(ex);
            tracker.stateProperty().set(PipelineObjectTracker.State.ERROR);
            trackerMap.remove(objId);
        });

        taskExecutor.execute(backgroundTask);

        return tracker;
    }

    /**
     * Package-private background task implementation for pipeline processing.
     * Provides progress tracking, ETA messages, and user-friendly titles.
     */
    static class PipelineBackgroundTask<I extends Identifiable, O extends Identifiable> extends BackgroundTask<O> {
        private final I inputObject;
        private final List<PipelineStage> stages;
        private final BiFunction<I, String, String> titleProvider;
        private final ProgressCounter progressCounter = new ProgressCounter();

        PipelineBackgroundTask(I inputObject,
                              List<PipelineStage> stages,
                              BiFunction<I, String, String> titleProvider,
                              BiConsumer<I, O> resultConsumer) {
            this.inputObject = inputObject;
            this.stages = stages;
            this.titleProvider = titleProvider;
        }

    @Override
    public O call() throws Exception {
        Identifiable result = inputObject;
        int currentStageIndex = 0;

        for (PipelineStage stage : stages) {
            // Update progress and title before stage execution
            updateProgress(currentStageIndex, stages.size());
            updateMessage(progressCounter.getMessage());
            setTitle(titleProvider.apply(inputObject, stage.getInfoString()));

            result = stage.process(result);
            currentStageIndex++;

            // Update progress after stage completion
            updateProgress(currentStageIndex, stages.size());
        }

        progressCounter.stop();
        return (O) result;
    }
    }
}
