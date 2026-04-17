package org.jabref.logic.importer.plaincitation;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.templates.AiTemplateRenderer;
import org.jabref.logic.ai.util.LlmResponseCleaner;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.fileformat.pdf.PdfImporterWithPlainCitationParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

public class LlmPlainCitationParser extends PdfImporterWithPlainCitationParser implements PlainCitationParser {
    private final ImportFormatPreferences importFormatPreferences;
    private final ChatModel chatModel;
    private final String citationParsingSystemMessageTemplate;

    public LlmPlainCitationParser(
            ImportFormatPreferences importFormatPreferences,
            String citationParsingSystemMessageTemplate,
            ChatModel chatModel
    ) {
        this.importFormatPreferences = importFormatPreferences;
        this.chatModel = chatModel;
        this.citationParsingSystemMessageTemplate = citationParsingSystemMessageTemplate;
    }

    @Override
    public String getId() {
        return "llm";
    }

    @Override
    public String getName() {
        return "LLM";
    }

    @Override
    public String getDescription() {
        return Localization.lang("LLM");
    }

    @Override
    public Optional<BibEntry> parsePlainCitation(String text) throws FetcherException {
        try {
            String bibtexString = getLlmResponse(text);
            return BibtexParser.singleFromString(bibtexString, importFormatPreferences);
        } catch (ParseException e) {
            throw new FetcherException("Could not parse BibTeX returned from LLM", e);
        }
    }

    @Override
    public List<BibEntry> parseMultiplePlainCitations(String text) throws FetcherException {
        try {
            String bibtexString = getLlmResponse(text);
            return parseBibEntryString(bibtexString);
        } catch (IOException e) {
            throw new FetcherException("Could not parse BibTeX returned from LLM", e);
        }
    }

    private List<BibEntry> parseBibEntryString(String text) throws IOException {
        BibtexParser parser = new BibtexParser(importFormatPreferences);
        ParserResult result = parser.parse(Reader.of(text));
        return result.getDatabase().getEntries();
    }

    private String getLlmResponse(String text) {
        String systemMessage = AiTemplateRenderer.renderCitationParsingSystemMessage(citationParsingSystemMessageTemplate);

        String response = chatModel.chat(
                List.of(
                        new SystemMessage(systemMessage),
                        new UserMessage(text)
                )
        ).aiMessage().text();

        return LlmResponseCleaner.clean(response);
    }
}

