package org.jabref.logic.ai.summarization.templates;

import java.util.function.Supplier;

import org.jabref.logic.ai.templates.AiTemplate;

import org.apache.velocity.VelocityContext;

public class SummarizationChunkUserMessageAiTemplate extends AiTemplate {
    public SummarizationChunkUserMessageAiTemplate(Supplier<String> source) {
        super(source);
    }

    public String render(String text) {
        VelocityContext context = makeContext();

        context.put("text", text);

        return render(context);
    }

    @Override
    public String getLogName() {
        return "SUMMARIZATION_CHUNK_USER_MESSAGE";
    }
}
