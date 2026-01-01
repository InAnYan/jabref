package org.jabref.model.ai.debug;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EmbeddingModelStep extends AiDebugStep {
    private final String modelName;
    private final int embeddingSize;

    @JsonCreator
    public EmbeddingModelStep(
            @JsonProperty("happenedAt") Instant happenedAt,
            @JsonProperty("timeSpent") Duration timeSpent,
            @JsonProperty("modelName") String modelName,
            @JsonProperty("embeddingSize") int embeddingSize
    ) {
        super(happenedAt, timeSpent);
        this.modelName = modelName;
        this.embeddingSize = embeddingSize;
    }

    public static EmbeddingModelStep withStepRecorder(
            StepRecorder stepRecorder,
            String modelName,
            int embeddingSize
    ) {
        return new EmbeddingModelStep(
                stepRecorder.getStart(),
                stepRecorder.stop(),
                modelName,
                embeddingSize
        );
    }

    public String getModelName() {
        return modelName;
    }

    public int getEmbeddingSize() {
        return embeddingSize;
    }
}
