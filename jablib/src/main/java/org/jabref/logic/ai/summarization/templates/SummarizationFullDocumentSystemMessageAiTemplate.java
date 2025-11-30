package org.jabref.logic.ai.summarization.templates;

import java.util.function.Supplier;

import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.model.ai.templating.AiTemplateKind;

import org.apache.velocity.VelocityContext;

public class SummarizationFullDocumentSystemMessageAiTemplate extends AiTemplate {
    public SummarizationFullDocumentSystemMessageAiTemplate(Supplier<String> source) {
        super(source);
    }

    public String render() {
        VelocityContext context = makeContext();
        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplateKind.SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE.name();
    }

    @Override
    public AiTemplateKind getKind() {
        return AiTemplateKind.SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE;
    }
}

