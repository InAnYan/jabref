package org.jabref.logic.importer.plaincitation;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.citationparsing.logic.ParseCitationsWithLlm;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.fileformat.pdf.PdfImporterWithPlainCitationParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.Result;
import org.jabref.model.entry.BibEntry;

public class LlmPlainCitationParser extends PdfImporterWithPlainCitationParser implements PlainCitationParser {
    private final ImportFormatPreferences importFormatPreferences;

    private final ChatModel chatModel;

    private final ParseCitationsWithLlm parseCitationsWithLlm;

    public LlmPlainCitationParser(
            ImportFormatPreferences importFormatPreferences,
            String citationParsingSystemMessageTemplate,
            ChatModel chatModel
    ) {
        this.importFormatPreferences = importFormatPreferences;

        this.chatModel = chatModel;

        this.parseCitationsWithLlm = new ParseCitationsWithLlm(
                importFormatPreferences,
                citationParsingSystemMessageTemplate
        );
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
            String string = parseCitationsWithLlm.getBibtexStringFromLlm(chatModel, text);
            return BibtexParser.singleFromString(string, importFormatPreferences);
        } catch (ParseException e) {
            throw new FetcherException("Could not parse BibTeX returned from LLM", e);
        }
    }

    @Override
    public List<BibEntry> parseMultiplePlainCitations(String text) throws FetcherException {
        Result<List<BibEntry>, IOException> result = parseCitationsWithLlm.parseMultiplePlainCitations(chatModel, text);

        if (result.isErr()) {
            throw new FetcherException("Could not parse BibTeX returned from LLM", result.getError());
        }

        return result.getValue();
    }
}

