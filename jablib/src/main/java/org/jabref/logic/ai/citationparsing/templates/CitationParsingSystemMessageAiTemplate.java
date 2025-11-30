package org.jabref.logic.ai.citationparsing.templates;

import java.util.function.Supplier;

import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.model.ai.templating.AiTemplateKind;

import org.apache.velocity.VelocityContext;

public class CitationParsingSystemMessageAiTemplate extends AiTemplate {
    public CitationParsingSystemMessageAiTemplate(Supplier<String> sourceSupplier) {
        super(sourceSupplier);
    }

    public String render() {
        VelocityContext context = makeContext();
        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplateKind.CITATION_PARSING_SYSTEM_MESSAGE.name();
    }

    @Override
    public AiTemplateKind getKind() {
        return AiTemplateKind.CITATION_PARSING_SYSTEM_MESSAGE;
    }
}
