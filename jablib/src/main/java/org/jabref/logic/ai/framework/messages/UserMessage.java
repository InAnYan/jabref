package org.jabref.logic.ai.framework.messages;

/**
 * Represents a user message in a chat conversation.
 */
public class UserMessage extends ChatMessage {

    /**
     * Creates a new user message.
     *
     * @param text the message content
     */
    public UserMessage(String text) {
        super(text);
    }
}
