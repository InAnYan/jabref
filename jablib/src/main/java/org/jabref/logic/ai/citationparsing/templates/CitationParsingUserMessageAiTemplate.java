package org.jabref.logic.ai.citationparsing.templates;

import java.util.function.Supplier;

import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.model.ai.templating.AiTemplateKind;

import org.apache.velocity.VelocityContext;

public class CitationParsingUserMessageAiTemplate extends AiTemplate {
    public CitationParsingUserMessageAiTemplate(Supplier<String> sourceSupplier) {
        super(sourceSupplier);
    }

    public String render(String citation) {
        VelocityContext context = makeContext();

        context.put("citation", citation);

        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplateKind.CITATION_PARSING_USER_MESSAGE.name();
    }

    @Override
    public AiTemplateKind getKind() {
        return AiTemplateKind.CITATION_PARSING_USER_MESSAGE;
    }
}
