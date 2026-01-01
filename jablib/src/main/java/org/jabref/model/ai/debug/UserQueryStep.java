package org.jabref.model.ai.debug;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserQueryStep extends AiDebugStep {
    private final String content;

    @JsonCreator
    public UserQueryStep(
            @JsonProperty("happenedAt") Instant happenedAt,
            @JsonProperty("timeSpent") Duration timeSpent,
            @JsonProperty("content") String content
    ) {
        super(happenedAt, timeSpent);
        this.content = content;
    }

    public static UserQueryStep now(
            String content
    ) {
        return new UserQueryStep(
                Instant.now(),
                Duration.ZERO,
                content
        );
    }

    public String getContent() {
        return content;
    }
}
