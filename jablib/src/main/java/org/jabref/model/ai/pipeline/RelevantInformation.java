package org.jabref.model.ai.pipeline;

import jakarta.annotation.Nullable;

/// Source is a citation key.
public record RelevantInformation(@Nullable String source, String text) {
}
