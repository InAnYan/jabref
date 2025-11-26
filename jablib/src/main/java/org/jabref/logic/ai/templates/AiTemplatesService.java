package org.jabref.logic.ai.templates;

import java.io.StringWriter;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.templating.AiTemplate;
import org.jabref.model.entry.CanonicalBibEntry;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class AiTemplatesService {
    private final AiPreferences aiPreferences;

    private final VelocityEngine velocityEngine = new VelocityEngine();
    private final VelocityContext baseContext = new VelocityContext();

    public AiTemplatesService(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        velocityEngine.init();

        baseContext.put("CanonicalBibEntry", CanonicalBibEntry.class);
    }

    public String makeCitationParsingSystemMessage() {
        VelocityContext context = new VelocityContext(baseContext);
        return makeTemplate(AiTemplate.CITATION_PARSING_SYSTEM_MESSAGE, context);
    }

    public String makeCitationParsingUserMessage(String citation) {
        VelocityContext context = new VelocityContext(baseContext);
        context.put("citation", citation);

        return makeTemplate(AiTemplate.CITATION_PARSING_USER_MESSAGE, context);
    }

    private String makeTemplate(AiTemplate template, VelocityContext context) {
        StringWriter writer = new StringWriter();

        velocityEngine.evaluate(context, writer, template.name(), aiPreferences.getTemplate(template));

        return writer.toString();
    }
}
