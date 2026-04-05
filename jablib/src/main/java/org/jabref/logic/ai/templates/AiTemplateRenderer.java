package org.jabref.logic.ai.templates;

import java.io.StringWriter;
import java.util.List;

import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.CanonicalBibEntry;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class AiTemplateRenderer {
    private static final VelocityEngine VELOCITY_ENGINE = new VelocityEngine();
    private static final VelocityContext BASE_CONTEXT = new VelocityContext();

    static {
        VELOCITY_ENGINE.init();
        BASE_CONTEXT.put("CanonicalBibEntry", CanonicalBibEntry.class);
    }

    private AiTemplateRenderer() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static String renderChattingSystemMessage(String templateSource, List<BibEntry> entries) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        context.put("entries", entries);
        return render(templateSource, "CHATTING_SYSTEM_MESSAGE", context);
    }

    public static String renderChattingUserMessage(String templateSource, List<BibEntry> entries, String message, List<RelevantInformation> excerpts) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        context.put("entries", entries);
        context.put("message", message);
        context.put("excerpts", excerpts);
        return render(templateSource, "CHATTING_USER_MESSAGE", context);
    }

    public static String renderSummarizationChunkSystemMessage(String templateSource) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        return render(templateSource, "SUMMARIZATION_CHUNK_SYSTEM_MESSAGE", context);
    }

    public static String renderSummarizationCombineSystemMessage(String templateSource) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        return render(templateSource, "SUMMARIZATION_COMBINE_SYSTEM_MESSAGE", context);
    }

    public static String renderSummarizationFullDocumentSystemMessage(String templateSource) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        return render(templateSource, "SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE", context);
    }

    public static String renderCitationParsingSystemMessage(String templateSource) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        return render(templateSource, "CITATION_PARSING_SYSTEM_MESSAGE", context);
    }

    private static String render(String templateSource, String logName, VelocityContext context) {
        StringWriter writer = new StringWriter();
        VELOCITY_ENGINE.evaluate(context, writer, logName, templateSource);
        return writer.toString();
    }
}

