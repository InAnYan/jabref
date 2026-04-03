package org.jabref.model.ai.chatting;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.pipeline.RelevantInformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessage {
    public enum Role {
        USER,
        AI,
        ERROR;

        public String getDisplayName() {
            return switch (this) {
                case USER ->
                        Localization.lang("User");
                case AI ->
                        Localization.lang("AI");
                case ERROR ->
                        Localization.lang("Error");
            };
        }
    }

    private final String id;
    private final Instant timestamp;
    private final Role role;
    private final String content;
    private final List<RelevantInformation> relevantInformation;

    @JsonCreator
    public ChatMessage(
            @JsonProperty("id") String id,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("role") Role role,
            @JsonProperty("content") String content,
            @JsonProperty("relevantInformation") List<RelevantInformation> relevantInformation
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.role = role;
        this.content = content;
        this.relevantInformation = relevantInformation;
    }

    public static ChatMessage userMessage(String content) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                Instant.now(),
                Role.USER,
                content,
                List.of()
        );
    }

    public static ChatMessage userMessage(Instant timestamp, String content) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                timestamp,
                Role.USER,
                content,
                List.of()
        );
    }

    public static ChatMessage aiMessage(
            String content,
            List<RelevantInformation> relevantInformation
    ) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                Instant.now(),
                Role.AI,
                content,
                relevantInformation
        );
    }

    public static ChatMessage errorMessage(Throwable throwable) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                Instant.now(),
                Role.ERROR,
                throwable.getMessage(),
                List.of()
        );
    }

    public String getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public List<RelevantInformation> getRelevantInformation() {
        return relevantInformation;
    }

    public Optional<dev.langchain4j.data.message.ChatMessage> toLangChainMessage() {
        return Optional.ofNullable(switch (role) {
            case USER ->
                    new dev.langchain4j.data.message.UserMessage(content);
            case AI ->
                    new dev.langchain4j.data.message.AiMessage(content);
            case ERROR ->
                    null;
        });
    }
}
