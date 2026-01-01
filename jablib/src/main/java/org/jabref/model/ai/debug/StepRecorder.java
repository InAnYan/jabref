package org.jabref.model.ai.debug;

import java.time.Duration;
import java.time.Instant;

public class StepRecorder {
    private final Instant start;

    public StepRecorder() {
        this.start = Instant.now();
    }

    public Duration stop() {
        return Duration.between(start, Instant.now());
    }

    public Instant getStart() {
        return start;
    }
}
