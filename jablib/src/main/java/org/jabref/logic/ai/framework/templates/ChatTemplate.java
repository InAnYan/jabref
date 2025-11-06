package org.jabref.logic.ai.framework.templates;

import org.apache.velocity.VelocityContext;

import java.util.List;

import org.jabref.logic.ai.framework.messages.ChatMessage;

/**
 * Template for generating entire chat conversations from multiple message templates.
 */
public class ChatTemplate {
    private final List<ChatMessageTemplate> messageTemplates;

    /**
     * Creates a new chat template.
     *
     * @param messageTemplates the ordered list of message templates to render
     */
    public ChatTemplate(List<ChatMessageTemplate> messageTemplates) {
        this.messageTemplates = List.copyOf(messageTemplates); // defensive copy
    }

    /**
     * Renders all message templates with the given context to create a chat conversation.
     *
     * @param context the Velocity context containing variables
     * @return the list of rendered chat messages
     */
    public List<ChatMessage> render(VelocityContext context) {
        return messageTemplates.stream()
                .map(template -> template.render(context))
                .toList();
    }
}
