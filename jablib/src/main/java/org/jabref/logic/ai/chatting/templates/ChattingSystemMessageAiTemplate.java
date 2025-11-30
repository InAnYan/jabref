package org.jabref.logic.ai.chatting.templates;

import java.util.List;
import java.util.function.Supplier;

import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.model.ai.templating.AiTemplateKind;
import org.jabref.model.entry.BibEntry;

import org.apache.velocity.VelocityContext;

public class ChattingSystemMessageAiTemplate extends AiTemplate {
    public ChattingSystemMessageAiTemplate(Supplier<String> source) {
        super(source);
    }

    public String render(List<BibEntry> entries) {
        VelocityContext context = makeContext();

        context.put("entries", entries);

        return render(context);
    }

    @Override
    public String getLogName() {
        return AiTemplateKind.CHATTING_SYSTEM_MESSAGE.name();
    }

    @Override
    public AiTemplateKind getKind() {
        return AiTemplateKind.CHATTING_SYSTEM_MESSAGE;
    }
}
