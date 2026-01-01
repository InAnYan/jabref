package org.jabref.model.ai.debug;

import java.time.Duration;
import java.time.Instant;

import jakarta.annotation.Nullable;

public class StepRecorder {
    private final Instant start;
    @Nullable private Duration timeSpent;

    public StepRecorder() {
        this.start = Instant.now();
    }

    public Duration stop() {
        if (timeSpent == null) {
            timeSpent = Duration.between(start, Instant.now());
        }

        return timeSpent;
    }

    public Instant getStart() {
        return start;
    }
}
