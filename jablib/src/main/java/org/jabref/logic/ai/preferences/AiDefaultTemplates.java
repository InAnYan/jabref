package org.jabref.logic.ai.preferences;

import java.util.Map;

import org.jabref.model.ai.templating.AiTemplate;

/// A collection of default AI templates.
///
/// This collection is made into a separate class (instead of putting into defaults at [org.jabref.logic.preferences.JabRefCliPreferences]),
/// because they are too big.
public class AiDefaultTemplates {
    private static final Map<AiTemplate, String> TEMPLATES = Map.of(
            AiTemplate.CHATTING_SYSTEM_MESSAGE, """
                    You are an AI assistant that analyses research papers. You answer questions about papers.
                    You will be supplied with the necessary information. The supplied information will contain mentions of papers in form '@citationKey'.
                    Whenever you refer to a paper, use its citation key in the same form with @ symbol. Whenever you find relevant information, always use the citation key.

                    Here are the papers you are analyzing:
                    #foreach( $entry in $entries )
                    ${CanonicalBibEntry.getCanonicalRepresentation($entry)}
                    #end""",

            AiTemplate.CHATTING_USER_MESSAGE, """
                    $message

                    Here is some relevant information for you:
                    #foreach( $excerpt in $excerpts )
                    ${excerpt.citationKey()}:
                    ${excerpt.text()}
                    #end""",

            AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE, """
                    Please provide an overview of the following text. It is a part of a scientific paper.
                    The bibEntrySummary should include the main objectives, methodologies used, key findings, and conclusions.
                    Mention any significant experiments, data, or discussions presented in the paper.""",

            AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE, "$text",

            AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE, """
                    You have written an overview of a scientific paper. You have been collecting notes from various parts
                    of the paper. Now your task is to combine all of the notes in one structured message.""",

            AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE, "$chunks",

            AiTemplate.CITATION_PARSING_SYSTEM_MESSAGE, "You are a bot to convert a plain text citation to a BibTeX entry. The user you talk to understands only BibTeX code, so provide it plainly without any wrappings.",
            AiTemplate.CITATION_PARSING_USER_MESSAGE, "Please convert this plain text citation to a BibTeX entry:\n$citation\nIn your output, please provide only BibTeX code as your message."
    );

    public static String getTemplate(AiTemplate template) {
        return TEMPLATES.get(template);
    }
}
