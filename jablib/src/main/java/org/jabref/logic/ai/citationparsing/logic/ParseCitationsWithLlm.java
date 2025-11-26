package org.jabref.logic.ai.citationparsing.logic;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.jabref.logic.ai.citationparsing.templates.CitationParsingSystemMessageTemplate;
import org.jabref.logic.ai.citationparsing.templates.CitationParsingUserMessageTemplate;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.util.Result;
import org.jabref.model.ai.chatting.ChatModelInfo;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

public class ParseCitationsWithLlm {
    private final ImportFormatPreferences importFormatPreferences;

    private final CitationParsingSystemMessageTemplate citationParsingSystemMessageTemplate;
    private final CitationParsingUserMessageTemplate citationParsingUserMessageTemplate;

    public ParseCitationsWithLlm(
            ImportFormatPreferences importFormatPreferences,
            CitationParsingSystemMessageTemplate citationParsingSystemMessageTemplate,
            CitationParsingUserMessageTemplate citationParsingUserMessageTemplate
    ) {
        this.importFormatPreferences = importFormatPreferences;

        this.citationParsingSystemMessageTemplate = citationParsingSystemMessageTemplate;
        this.citationParsingUserMessageTemplate = citationParsingUserMessageTemplate;
    }

    public Result<List<BibEntry>, IOException> parseMultiplePlainCitations(
            ChatModelInfo chatModelInfo,
            String text
    ) {
        String systemMessage = citationParsingSystemMessageTemplate.render();
        String userMessage = citationParsingUserMessageTemplate.render(text);

        // TODO: Clean possibly of backticks.
        String llmResult = chatModelInfo.chatModel().chat(
                List.of(
                        new SystemMessage(systemMessage),
                        new UserMessage(userMessage)
                )
        ).aiMessage().text();

        return parseBibEntryString(llmResult);
    }

    private Result<List<BibEntry>, IOException> parseBibEntryString(String text) {
        Reader reader = Reader.of(text);
        BibtexParser parser = new BibtexParser(importFormatPreferences);
        ParserResult result;
        try {
            result = parser.parse(reader);
        } catch (IOException e) {
            return Result.err(e);
        }

        return Result.ok(result.getDatabase().getEntries());
    }

    public String getBibtexStringFromLlm(
            ChatModelInfo chatModelInfo,
            String searchQuery
    ) {
        String systemMessage = citationParsingSystemMessageTemplate.render();
        String userMessage = citationParsingUserMessageTemplate.render(searchQuery);

        return chatModelInfo.chatModel().chat(
                List.of(
                        new SystemMessage(systemMessage),
                        new UserMessage(userMessage)
                )
        ).aiMessage().text();
    }
}
