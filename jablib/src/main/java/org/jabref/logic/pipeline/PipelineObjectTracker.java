package org.jabref.logic.pipeline;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Tracks the state of an object being processed through a pipeline.
 * <p>
 * Can be in one of three states:
 * - Processing: The object is currently being processed
 * - Error: An error occurred during processing
 * - Finished: Processing completed successfully
 *
 * @param <I> the type of the input object
 * @param <O> the type of the processed object
 */
public final class PipelineObjectTracker<I extends Identifiable, O extends Identifiable> {
    /** The original input object being processed. */
    private final I inputObject;
    /** Current processing state. */
    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.PROCESSING);
    /** Result of successful processing. */
    private final ObjectProperty<O> result = new SimpleObjectProperty<>();
    /** Exception if processing failed. */
    private final ObjectProperty<Exception> error = new SimpleObjectProperty<>();

    /**
     * Creates a tracker for the given input object, starting in PROCESSING state.
     *
     * @param inputObject the object being processed
     */
    public PipelineObjectTracker(I inputObject) {
        this.inputObject = inputObject;
    }

    /**
     * Returns the original input object being processed.
     */
    public I getInputObject() {
        return inputObject;
    }

    /**
     * Returns the current state of the tracker.
     */
    public State getState() {
        return state.get();
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    /**
     * Returns true if processing is complete (either finished or error).
     */
    public boolean isComplete() {
        return getState() != State.PROCESSING;
    }

    /**
     * Returns true if processing finished successfully.
     */
    public boolean isFinished() {
        return getState() == State.FINISHED;
    }

    /**
     * Returns true if an error occurred during processing.
     */
    public boolean isError() {
        return getState() == State.ERROR;
    }

    /**
     * Returns true if processing is still in progress.
     */
    public boolean isProcessing() {
        return getState() == State.PROCESSING;
    }

    /**
     * Returns the result if finished, otherwise empty.
     */
    public Optional<O> getResult() {
        return Optional.ofNullable(result.get());
    }

    /**
     * JavaFX property for the processing result.
     */
    public ObjectProperty<O> resultProperty() {
        return result;
    }

    /**
     * Returns the error if an error occurred, otherwise empty.
     */
    public Optional<Exception> getError() {
        return Optional.ofNullable(error.get());
    }

    public ObjectProperty<Exception> errorProperty() {
        return error;
    }

    /**
     * The possible states of a pipeline object tracker.
     */
    public enum State {
        PROCESSING,
        ERROR,
        FINISHED
    }
}
