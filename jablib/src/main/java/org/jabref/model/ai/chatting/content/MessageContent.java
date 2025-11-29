package org.jabref.model.ai.chatting.content;

import java.io.Serial;
import java.io.Serializable;

public abstract class MessageContent implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final String content;

    public MessageContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public abstract MessageContentType getType();
}
