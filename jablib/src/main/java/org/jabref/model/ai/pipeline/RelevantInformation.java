package org.jabref.model.ai.pipeline;

import java.util.List;

// Sources are citation key.
public record RelevantInformation(List<String> sources, String text) {
}
