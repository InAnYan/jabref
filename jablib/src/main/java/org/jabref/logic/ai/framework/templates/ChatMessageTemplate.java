package org.jabref.logic.ai.framework.templates;

import org.apache.velocity.VelocityContext;

import org.jabref.logic.ai.framework.messages.ChatMessage;

/**
 * Template for generating chat messages.
 */
public class ChatMessageTemplate extends PromptTemplate {

    /**
     * Creates a new chat message template.
     *
     * @param template the Velocity template string
     */
    public ChatMessageTemplate(String template) {
        super(template);
    }

    /**
     * Renders the template with the given context.
     *
     * @param context the Velocity context containing variables
     * @return the rendered chat message
     */
    public ChatMessage render(VelocityContext context) {
        String content = super.apply(context);
        return new ChatMessage(content); // Default fallback
    }
}
