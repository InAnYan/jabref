package org.jabref.model.ai;

import java.time.Instant;

import org.jabref.model.ai.llm.AiProvider;

/// Metadata about the AI model used to produce a particular result.
///
/// This record is passed to all exporter implementations so that outputs can include
/// provenance information (which provider and model generated the content, and when).
///
/// Use {@link #empty()} when no model information is available.
public record AiMetadata(AiProvider aiProvider, String model, Instant timestamp) {
    /// Returns an {@code AiMetadata} with no provider or model information.
    public static AiMetadata empty() {
        return new AiMetadata(null, "", Instant.now());
    }
}
