package org.jabref.logic.ai.chatting.templates;

import java.util.List;
import java.util.function.Supplier;

import org.jabref.logic.ai.templates.Template;
import org.jabref.model.ai.templating.AiTemplate;
import org.jabref.model.entry.BibEntry;

import org.apache.velocity.VelocityContext;

public class ChattingSystemMessageTemplate extends Template {
    public ChattingSystemMessageTemplate(Supplier<String> source) {
        super(source);
    }

    public String render(List<BibEntry> entries) {
        VelocityContext context = makeContext();

        context.put("entries", entries);

        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplate.CHATTING_SYSTEM_MESSAGE.name();
    }
}
