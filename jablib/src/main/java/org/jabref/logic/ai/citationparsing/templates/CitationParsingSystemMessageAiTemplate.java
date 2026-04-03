package org.jabref.logic.ai.citationparsing.templates;

import org.jabref.logic.ai.templates.AiTemplate;

import org.apache.velocity.VelocityContext;

public class CitationParsingSystemMessageAiTemplate extends AiTemplate {
    public CitationParsingSystemMessageAiTemplate(String source) {
        super(source);
    }

    public String render() {
        VelocityContext context = makeContext();
        return render(context);
    }

    @Override
    public String getLogName() {
        return "CITATION_PARSING_SYSTEM_MESSAGE";
    }
}
