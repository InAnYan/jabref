package org.jabref.model.ai.chatting;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.pipeline.RelevantInformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

public record ChatMessage(String id, Instant timestamp, Role role, String content, List<RelevantInformation> relevantInformation) {
    public enum Role {
        SYSTEM,
        USER,
        AI,
        ERROR;

        public String getDisplayName() {
            return switch (this) {
                case SYSTEM ->
                        Localization.lang("System");
                case USER ->
                        Localization.lang("User");
                case AI ->
                        Localization.lang("AI");
                case ERROR ->
                        Localization.lang("Error");
            };
        }

        public boolean canRegenerate() {
            return this == AI || this == ERROR;
        }
    }

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
        return userMessage(Instant.now(), content);
    }

    public static ChatMessage userMessage(Instant timestamp, String content) {
        return dummyChatMessage(Role.USER, content, timestamp, List.of());
    }

    public static ChatMessage systemMessage(String content) {
        return systemMessage(Instant.now(), content);
    }

    public static ChatMessage systemMessage(Instant timestamp, String content) {
        return dummyChatMessage(Role.SYSTEM, content, timestamp, List.of());
    }

    public static ChatMessage aiMessage(
            String content,
            List<RelevantInformation> relevantInformation
    ) {
        return dummyChatMessage(Role.AI, content, Instant.now(), relevantInformation);
    }

    public static ChatMessage errorMessage(Throwable throwable) {
        return dummyChatMessage(Role.ERROR, throwable.getMessage(), Instant.now(), List.of());
    }

    private static ChatMessage dummyChatMessage(
            Role role,
            String content,
            Instant timestamp,
            List<RelevantInformation> relevantInformation
    ) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                timestamp,
                role,
                content,
                relevantInformation
        );
    }

    public Optional<dev.langchain4j.data.message.ChatMessage> toLangChainMessage() {
        return Optional.ofNullable(switch (role) {
            case SYSTEM ->
                    new SystemMessage(content);
            case USER ->
                    new UserMessage(content);
            case AI ->
                    new AiMessage(content);
            case ERROR ->
                    null;
        });
    }
}
