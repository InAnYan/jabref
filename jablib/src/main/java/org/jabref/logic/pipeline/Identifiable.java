package org.jabref.logic.pipeline;

/**
 * Marker interface for objects that can be processed through a pipeline.
 * <p>
 * ID is used, for example, for pipeline stages that cache information.
 */
public interface Identifiable {
    /**
     * Return an ID that uniquely represents this object.
     * <p>
     * For persitence, the returned string must stay the same between
     * different runs of the program.
     */
    String getId();
}


