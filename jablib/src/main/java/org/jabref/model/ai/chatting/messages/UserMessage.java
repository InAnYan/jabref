package org.jabref.model.ai.chatting.messages;

import java.time.Instant;

public class UserMessage extends JabRefChatMessage {
    public UserMessage(Instant createdAt, String content) {
        super(createdAt, content);
    }

    @Override
    public JabRefChatMessageType getType() {
        return JabRefChatMessageType.USER_MESSAGE;
    }
}
