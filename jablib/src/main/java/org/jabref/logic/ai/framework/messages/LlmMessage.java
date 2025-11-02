package org.jabref.logic.ai.framework.messages;

/**
 * Represents an LLM response message in a chat conversation.
 */
public class LlmMessage extends ChatMessage {

    /**
     * Creates a new LLM message.
     *
     * @param text the message content
     */
    public LlmMessage(String text) {
        super(text);
    }
}
