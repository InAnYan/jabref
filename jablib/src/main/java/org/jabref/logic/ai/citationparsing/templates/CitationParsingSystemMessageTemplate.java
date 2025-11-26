package org.jabref.logic.ai.citationparsing.templates;

import java.util.function.Supplier;

import org.jabref.logic.ai.templates.Template;
import org.jabref.model.ai.templating.AiTemplate;

import org.apache.velocity.VelocityContext;

public class CitationParsingSystemMessageTemplate extends Template {
    public CitationParsingSystemMessageTemplate(Supplier<String> sourceSupplier) {
        super(sourceSupplier);
    }

    public String render() {
        VelocityContext context = makeContext();
        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplate.CITATION_PARSING_SYSTEM_MESSAGE.name();
    }
}
