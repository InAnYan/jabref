package org.jabref.model.ai.debug;

import java.time.Duration;
import java.time.Instant;

import org.jabref.model.ai.embeddings.EmbeddingSimilarityMetric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VectorDatabaseQueryStep extends AiDebugStep {
    private final EmbeddingSimilarityMetric similarityMetric;

    @JsonCreator
    public VectorDatabaseQueryStep(
            @JsonProperty("happenedAt") Instant happenedAt,
            @JsonProperty("timeSpent") Duration timeSpent,
            @JsonProperty("similarityMetric") EmbeddingSimilarityMetric similarityMetric
    ) {
        super(happenedAt, timeSpent);
        this.similarityMetric = similarityMetric;
    }

    public static VectorDatabaseQueryStep withStepRecorder(
            StepRecorder stepRecorder,
            EmbeddingSimilarityMetric similarityMetric
    ) {
        return new VectorDatabaseQueryStep(
                stepRecorder.getStart(),
                stepRecorder.stop(),
                similarityMetric
        );
    }

    public EmbeddingSimilarityMetric getSimilarityMetric() {
        return similarityMetric;
    }
}
