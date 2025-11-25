package org.jabref.logic.ai.summarization.templates;

import org.jabref.logic.ai.templates.Template;
import org.jabref.model.ai.templating.AiTemplate;

import org.apache.velocity.VelocityContext;

public class SummarizationChunkUserMessageTemplate extends Template {
    public SummarizationChunkUserMessageTemplate(String source) {
        super(source);
    }

    public String render(String text) {
        VelocityContext context = makeContext();

        context.put("text", text);

        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE.name();
    }
}
