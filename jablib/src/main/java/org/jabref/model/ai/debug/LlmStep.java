package org.jabref.model.ai.debug;

import java.time.Duration;
import java.time.Instant;

import org.jabref.model.ai.llm.AiProvider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LlmStep extends AiDebugStep {
    private final AiProvider aiProvider;
    private final String modelName;

    @JsonCreator
    public LlmStep(
            @JsonProperty("happenedAt") Instant happenedAt,
            @JsonProperty("timeSpent") Duration timeSpent,
            @JsonProperty("aiProvider") AiProvider aiProvider,
            @JsonProperty("modelName") String modelName
    ) {
        super(happenedAt, timeSpent);
        this.aiProvider = aiProvider;
        this.modelName = modelName;
    }

    public static LlmStep withStepRecorder(
            StepRecorder stepRecorder,
            AiProvider aiProvider,
            String modelName
    ) {
        return new LlmStep(
                stepRecorder.getStart(),
                stepRecorder.stop(),
                aiProvider,
                modelName
        );
    }

    public AiProvider getAiProvider() {
        return aiProvider;
    }

    public String getModelName() {
        return modelName;
    }
}
