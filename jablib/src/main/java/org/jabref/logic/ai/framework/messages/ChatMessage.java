package org.jabref.logic.ai.framework.messages;

/**
 * Base class for all chat messages in conversations.
 */
public class ChatMessage {
    private final String text;

    /**
     * Creates a new chat message.
     *
     * @param text the message content
     */
    public ChatMessage(String text) {
        this.text = text;
    }

    /**
     * Returns the message text.
     *
     * @return the message content
     */
    public String getText() {
        return text;
    }
}
