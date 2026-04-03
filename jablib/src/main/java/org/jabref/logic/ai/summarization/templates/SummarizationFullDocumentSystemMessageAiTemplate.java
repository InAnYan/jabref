package org.jabref.logic.ai.summarization.templates;

import org.jabref.logic.ai.templates.AiTemplate;

import org.apache.velocity.VelocityContext;

public class SummarizationFullDocumentSystemMessageAiTemplate extends AiTemplate {
    public SummarizationFullDocumentSystemMessageAiTemplate(String source) {
        super(source);
    }

    public String render() {
        VelocityContext context = makeContext();
        return render(context);
    }

    @Override
    public String getLogName() {
        return "SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE";
    }
}

