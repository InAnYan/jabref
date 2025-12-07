package org.jabref.model.ai.pipeline;

import java.util.List;

// Sources are citation key.
// Used only in AI replies in one library?
public record RelevantInformation(List<String> sources, String text) {
}
