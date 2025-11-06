package org.jabref.logic.ai.framework.messages;

/**
 * Represents a system message in a chat conversation.
 */
public class SystemMessage extends ChatMessage {

    /**
     * Creates a new system message.
     *
     * @param text the message content
     */
    public SystemMessage(String text) {
        super(text);
    }
}
