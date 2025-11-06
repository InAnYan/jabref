package org.jabref.logic.ai.util;

import org.jabref.logic.ai.framework.messages.ChatMessage;

/**
 * Class representing an error from AI side.
 * This is a dummy class that extends from our {@link ChatMessage}.
 * The primary use of this class is to be stored in a chat history and displayed in the UI.
 */
public class ErrorMessage extends ChatMessage {

    public ErrorMessage(String text) {
        super(text);
    }
}
