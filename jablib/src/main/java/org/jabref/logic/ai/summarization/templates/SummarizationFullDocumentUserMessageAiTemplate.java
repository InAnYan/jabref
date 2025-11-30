package org.jabref.logic.ai.summarization.templates;

import java.util.function.Supplier;

import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.model.ai.templating.AiTemplateKind;

import org.apache.velocity.VelocityContext;

public class SummarizationFullDocumentUserMessageAiTemplate extends AiTemplate {
    public SummarizationFullDocumentUserMessageAiTemplate(Supplier<String> source) {
        super(source);
    }

    public String render(String text) {
        VelocityContext context = makeContext();

        context.put("text", text);

        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplateKind.SUMMARIZATION_FULL_DOCUMENT_USER_MESSAGE.name();
    }

    @Override
    public AiTemplateKind getKind() {
        return AiTemplateKind.SUMMARIZATION_FULL_DOCUMENT_USER_MESSAGE;
    }
}

