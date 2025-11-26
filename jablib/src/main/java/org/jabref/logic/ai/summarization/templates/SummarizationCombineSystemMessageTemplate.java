package org.jabref.logic.ai.summarization.templates;

import java.util.function.Supplier;

import org.jabref.logic.ai.templates.Template;
import org.jabref.model.ai.templating.AiTemplate;

import org.apache.velocity.VelocityContext;

public class SummarizationCombineSystemMessageTemplate extends Template {
    public SummarizationCombineSystemMessageTemplate(Supplier<String> source) {
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
