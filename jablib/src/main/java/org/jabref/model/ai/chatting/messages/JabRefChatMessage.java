package org.jabref.model.ai.chatting.messages;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/// JabRef's notion of chat messages. Extended langchain and more to what a user sees.
public abstract class JabRefChatMessage implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    // TODO: Should store ID?

    private final Instant createdAt;
    private final String content;

    public JabRefChatMessage(
            Instant createdAt,
            String content
    ) {
        this.createdAt = createdAt;
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getContent() {
        return content;
    }

    public abstract JabRefChatMessageType getType();
}
