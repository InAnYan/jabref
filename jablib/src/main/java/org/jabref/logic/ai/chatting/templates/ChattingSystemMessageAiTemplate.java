package org.jabref.logic.ai.chatting.templates;

import java.util.List;

import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.model.entry.BibEntry;

import org.apache.velocity.VelocityContext;

public class ChattingSystemMessageAiTemplate extends AiTemplate {
    public ChattingSystemMessageAiTemplate(String source) {
        super(source);
    }

    public String render(List<BibEntry> entries) {
        VelocityContext context = makeContext();

        context.put("entries", entries);

        return render(context);
    }

    @Override
    public String getLogName() {
        return "CHATTING_SYSTEM_MESSAGE";
    }
}
