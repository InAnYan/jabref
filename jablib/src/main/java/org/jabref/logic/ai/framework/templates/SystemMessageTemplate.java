package org.jabref.logic.ai.framework.templates;

import org.apache.velocity.VelocityContext;

import org.jabref.logic.ai.framework.messages.ChatMessage;
import org.jabref.logic.ai.framework.messages.SystemMessage;

/**
 * Template for generating system messages.
 */
public class SystemMessageTemplate extends ChatMessageTemplate {

    /**
     * Creates a new system message template.
     *
     * @param template the Velocity template string
     */
    public SystemMessageTemplate(String template) {
        super(template);
    }

    @Override
    public ChatMessage render(VelocityContext context) {
        String content = super.apply(context);
        return new SystemMessage(content);
    }
}
