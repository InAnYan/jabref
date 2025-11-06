package org.jabref.logic.ai.framework.templates;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;

/**
 * Base class for prompt templates using Apache Velocity templating.
 */
public class PromptTemplate {
    private static final VelocityEngine VELOCITY_ENGINE = new VelocityEngine();

    static {
        VELOCITY_ENGINE.init();
    }

    private final String template;

    /**
     * Creates a new prompt template.
     *
     * @param template the Velocity template string
     */
    public PromptTemplate(String template) {
        this.template = template;
    }

    /**
     * Applies the template with the given Velocity context.
     *
     * @param context the Velocity context containing variables
     * @return the rendered template string
     */
    public String apply(VelocityContext context) {
        StringWriter writer = new StringWriter();
        VELOCITY_ENGINE.evaluate(context, writer, "template", template);
        return writer.toString();
    }
}
