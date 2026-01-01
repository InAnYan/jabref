package org.jabref.model.ai.debug;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserQueryStep.class, name = "userQuery"),
        @JsonSubTypes.Type(value = EmbeddingModelStep.class, name = "embeddingModel"),
        @JsonSubTypes.Type(value = VectorDatabaseQueryStep.class, name = "vectorDatabaseQuery"),
        @JsonSubTypes.Type(value = LlmStep.class, name = "llm"),
})
public abstract class AiDebugStep {
    private final Instant happenedAt;
    private final Duration timeSpent;

    @JsonCreator
    public AiDebugStep(
            @JsonProperty("happenedAt") Instant happenedAt,
            @JsonProperty("timeSpent") Duration timeSpent
    ) {
        this.happenedAt = happenedAt;
        this.timeSpent = timeSpent;
    }

    public Instant getHappenedAt() {
        return happenedAt;
    }

    public Duration getTimeSpent() {
        return timeSpent;
    }
}
