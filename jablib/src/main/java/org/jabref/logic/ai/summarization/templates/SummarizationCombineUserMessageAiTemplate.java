package org.jabref.logic.ai.summarization.templates;

import java.util.List;
import java.util.function.Supplier;

import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.model.ai.templating.AiTemplateKind;

import org.apache.velocity.VelocityContext;

public class SummarizationCombineUserMessageAiTemplate extends AiTemplate {
    public SummarizationCombineUserMessageAiTemplate(Supplier<String> source) {
        super(source);
    }

    public String render(List<String> chunks) {
        VelocityContext context = makeContext();

        context.put("chunks", chunks);

        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplateKind.SUMMARIZATION_COMBINE_USER_MESSAGE.name();
    }

    @Override
    public AiTemplateKind getKind() {
        return AiTemplateKind.SUMMARIZATION_COMBINE_USER_MESSAGE;
    }
}
