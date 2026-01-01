package org.jabref.model.ai.debug;

import java.time.Duration;
import java.time.Instant;

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
    public Instant happenedAt;
    public Duration timeSpent;

    public Instant getHappenedAt() {
        return happenedAt;
    }

    public void setHappenedAt(Instant happenedAt) {
        this.happenedAt = happenedAt;
    }

    public Duration getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(Duration timeSpent) {
        this.timeSpent = timeSpent;
    }
}
