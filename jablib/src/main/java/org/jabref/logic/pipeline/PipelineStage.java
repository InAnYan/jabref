package org.jabref.logic.pipeline;

import org.jabref.logic.l10n.Localization;

/**
 * Base class for pipeline processing stages.
 * <p>
 * Each stage transforms an input object of type I into an output object of type O.
 * Stages are chained together in a pipeline to perform complex processing workflows.
 */
public abstract class PipelineStage<I extends Identifiable, O extends Identifiable> {
    /** Descriptive information about what this stage does. */
    private final String infoString;

    /**
     * Creates a pipeline stage with the given descriptive information.
     *
     * @param infoString description of what this stage does (e.g., "Parsing document", "Validating data")
     */
    protected PipelineStage(String infoString) {
        this.infoString = infoString;
    }

    /**
     * Creates a pipeline stage with default descriptive information.
     */
    protected PipelineStage() {
        this(Localization.lang("Processing"));
    }

    /**
     * Returns the descriptive information about what this stage does.
     *
     * @return the info string for this stage
     */
    public String getInfoString() {
        return infoString;
    }

    /**
     * Processes the given input object and returns the transformed output.
     * <p>
     * This method defines the core transformation logic for the stage.
     * Implementations should be stateless and thread-safe.
     *
     * @param obj the input object to process
     * @return the transformed output object
     */
    public abstract O process(I obj);
}
