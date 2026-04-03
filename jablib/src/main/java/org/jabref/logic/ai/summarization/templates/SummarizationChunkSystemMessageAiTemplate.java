package org.jabref.logic.ai.summarization.templates;

import java.util.function.Supplier;

import org.jabref.logic.ai.templates.AiTemplate;

import org.apache.velocity.VelocityContext;

public class SummarizationChunkSystemMessageAiTemplate extends AiTemplate {
    public SummarizationChunkSystemMessageAiTemplate(Supplier<String> source) {
        super(source);
    }

    public String render() {
        VelocityContext context = makeContext();
        return render(context);
    }

    @Override
    public String getLogName() {
        return "SUMMARIZATION_CHUNK_SYSTEM_MESSAGE";
    }
}
