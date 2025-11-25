package org.jabref.model.ai.summarization;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.jabref.model.ai.chatting.AiProvider;

public record BibEntrySummary(
        LocalDateTime timestamp,
        AiProvider aiProvider,
        String model,
        SummarizationAlgorithmName summarizationAlgorithm,
        String content
) implements Serializable {
}
