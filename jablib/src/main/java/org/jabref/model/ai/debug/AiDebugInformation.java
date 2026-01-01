package org.jabref.model.ai.debug;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public class AiDebugInformation {
    private final List<AiDebugStep> steps;
    // Might be changed.
    @Nullable private String error;

    @JsonCreator
    public AiDebugInformation(
            @JsonProperty("steps") List<AiDebugStep> steps,
            @JsonProperty("error") @Nullable String error
    ) {
        this.steps = steps;
        this.error = error;
    }

    public AiDebugInformation() {
        this.steps = new ArrayList<>();
        this.error = null;
    }

    public List<AiDebugStep> getSteps() {
        return steps;
    }

    @Nullable
    public String getError() {
        return error;
    }

    public void setError(@Nullable String error) {
        this.error = error;
    }

    public Duration calculateTimeSpent() {
        return steps.stream().map(AiDebugStep::getTimeSpent).reduce(Duration.ZERO, Duration::plus);
    }
}
