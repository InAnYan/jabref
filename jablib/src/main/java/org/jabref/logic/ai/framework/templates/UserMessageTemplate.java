package org.jabref.logic.ai.framework.templates;

import org.apache.velocity.VelocityContext;

import org.jabref.logic.ai.framework.messages.ChatMessage;
import org.jabref.logic.ai.framework.messages.UserMessage;

/**
 * Template for generating user messages.
 */
public class UserMessageTemplate extends ChatMessageTemplate {

    /**
     * Creates a new user message template.
     *
     * @param template the Velocity template string
     */
    public UserMessageTemplate(String template) {
        super(template);
    }

    @Override
    public ChatMessage render(VelocityContext context) {
        String content = super.apply(context);
        return new UserMessage(content);
    }
}
