package org.jabref.logic.ai.templates;

import java.io.StringWriter;

import org.jabref.model.entry.CanonicalBibEntry;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public abstract class AiTemplate {
    private static final VelocityEngine VELOCITY_ENGINE = new VelocityEngine();
    private static final VelocityContext BASE_CONTEXT = new VelocityContext();

    static {
        VELOCITY_ENGINE.init();

        BASE_CONTEXT.put("CanonicalBibEntry", CanonicalBibEntry.class);
    }

    private final String source;

    public AiTemplate(String source) {
        this.source = source;
    }

    protected String render(VelocityContext context) {
        StringWriter writer = new StringWriter();
        VELOCITY_ENGINE.evaluate(context, writer, getLogName(), source);
        return writer.toString();
    }

    protected VelocityContext makeContext() {
        return new VelocityContext(BASE_CONTEXT);
    }

    public String getSource() {
        return source;
    }

    // Required by Velocity.
    public abstract String getLogName();
}
