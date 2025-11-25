package org.jabref.logic.ai.summarization.templates;

import org.jabref.logic.ai.templates.Template;
import org.jabref.model.ai.templating.AiTemplate;

import org.apache.velocity.VelocityContext;

public class SummarizationChunkSystemMessageTemplate extends Template {
    public SummarizationChunkSystemMessageTemplate(String source) {
        super(source);
    }

    public String render() {
        VelocityContext context = makeContext();
        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE.name();
    }
}
