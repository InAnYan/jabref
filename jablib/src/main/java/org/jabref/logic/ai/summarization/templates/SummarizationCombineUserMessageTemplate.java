package org.jabref.logic.ai.summarization.templates;

import java.util.List;

import org.jabref.logic.ai.templates.Template;
import org.jabref.model.ai.templating.AiTemplate;

import org.apache.velocity.VelocityContext;

public class SummarizationCombineUserMessageTemplate extends Template {
    public SummarizationCombineUserMessageTemplate(String source) {
        super(source);
    }

    public String render(List<String> chunks) {
        VelocityContext context = makeContext();

        context.put("chunks", chunks);

        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE.name();
    }
}
