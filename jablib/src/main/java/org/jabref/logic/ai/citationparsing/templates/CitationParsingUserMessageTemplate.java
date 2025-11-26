package org.jabref.logic.ai.citationparsing.templates;

import java.util.function.Supplier;

import org.jabref.logic.ai.templates.Template;
import org.jabref.model.ai.templating.AiTemplate;

import org.apache.velocity.VelocityContext;

public class CitationParsingUserMessageTemplate extends Template {
    public CitationParsingUserMessageTemplate(Supplier<String> sourceSupplier) {
        super(sourceSupplier);
    }

    public String render(String citation) {
        VelocityContext context = makeContext();

        context.put("citation", citation);

        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplate.CITATION_PARSING_USER_MESSAGE.name();
    }
}
