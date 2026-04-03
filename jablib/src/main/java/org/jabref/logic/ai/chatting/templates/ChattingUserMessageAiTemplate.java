package org.jabref.logic.ai.chatting.templates;

import java.util.List;

import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;

import org.apache.velocity.VelocityContext;

public class ChattingUserMessageAiTemplate extends AiTemplate {
    public ChattingUserMessageAiTemplate(String source) {
        super(source);
    }

    public String render(List<BibEntry> entries, String message, List<RelevantInformation> excerpts) {
        VelocityContext context = makeContext();

        context.put("entries", entries);
        context.put("message", message);
        context.put("excerpts", excerpts);

        return render(context);
    }

    @Override
    public String getLogName() {
        return "CHATTING_USER_MESSAGE";
    }
}
