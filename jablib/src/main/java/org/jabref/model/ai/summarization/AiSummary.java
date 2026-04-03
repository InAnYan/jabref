package org.jabref.model.ai.summarization;

import java.time.Instant;

import org.jabref.model.ai.llm.AiProvider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSummary(
        Instant timestamp,
        AiProvider aiProvider,
        String model,
        SummarizatorKind summarizationAlgorithm,
        String content
) {
    @JsonCreator()
    public AiSummary(
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("aiProvider") AiProvider aiProvider,
            @JsonProperty("model") String model,
            @JsonProperty("summarizationAlgorithm") SummarizatorKind summarizationAlgorithm,
            @JsonProperty("content") String content
    ) {
        this.timestamp = timestamp;
        this.aiProvider = aiProvider;
        this.model = model;
        this.summarizationAlgorithm = summarizationAlgorithm;
        this.content = content;
    }
}
