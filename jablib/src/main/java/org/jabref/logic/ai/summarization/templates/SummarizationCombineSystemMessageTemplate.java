package org.jabref.logic.ai.summarization.templates;

import org.jabref.logic.ai.templates.Template;
import org.jabref.model.ai.templating.AiTemplate;

import org.apache.velocity.VelocityContext;

public class SummarizationCombineSystemMessageTemplate extends Template {
    public SummarizationCombineSystemMessageTemplate(String source) {
        super(source);
    }

    public String render() {
        VelocityContext context = makeContext();
        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE.name();
    }
}
