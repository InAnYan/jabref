package org.jabref.logic.ai.framework.templates;

import org.apache.velocity.VelocityContext;

import org.jabref.logic.ai.framework.messages.ChatMessage;
import org.jabref.logic.ai.framework.messages.LlmMessage;

/**
 * Template for generating LLM messages.
 */
public class LlmMessageTemplate extends ChatMessageTemplate {

    /**
     * Creates a new LLM message template.
     *
     * @param template the Velocity template string
     */
    public LlmMessageTemplate(String template) {
        super(template);
    }

    @Override
    public ChatMessage render(VelocityContext context) {
        String content = super.apply(context);
        return new LlmMessage(content);
    }
}
