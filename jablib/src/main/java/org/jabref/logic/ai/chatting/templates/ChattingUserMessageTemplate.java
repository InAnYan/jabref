package org.jabref.logic.ai.chatting.templates;

import java.util.List;
import java.util.function.Supplier;

import org.jabref.logic.ai.templates.Template;
import org.jabref.model.ai.rag.PaperExcerpt;
import org.jabref.model.ai.templating.AiTemplate;
import org.jabref.model.entry.BibEntry;

import org.apache.velocity.VelocityContext;

public class ChattingUserMessageTemplate extends Template {
    public ChattingUserMessageTemplate(Supplier<String> source) {
        super(source);
    }

    public String render(List<BibEntry> entries, String message, List<PaperExcerpt> excerpts) {
        VelocityContext context = makeContext();

        context.put("entries", entries);
        context.put("message", message);
        context.put("excerpts", excerpts);

        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplate.CHATTING_USER_MESSAGE.name();
    }
}
