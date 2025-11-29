package org.jabref.model.ai.summarization;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.jabref.model.ai.llm.AiProvider;

public record BibEntrySummary(
        LocalDateTime timestamp,
        AiProvider aiProvider,
        String model,
        SummarizatorKind summarizationAlgorithm,
        String content
) implements Serializable {
}
