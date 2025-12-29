package org.jabref.logic.ai.rag.logic;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.logic.parsing.UniversalContentParser;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;

public class FullDocumentAnswerEngine implements AnswerEngine {
    private final FilePreferences filePreferences;

    // TODO: Add dependency on parsing.
    private final UniversalContentParser universalContentParser = new UniversalContentParser();

    public FullDocumentAnswerEngine(FilePreferences filePreferences) {
        this.filePreferences = filePreferences;
    }

    @Override
    public List<RelevantInformation> process(
            LongTaskInfo longTaskInfo,
            String query,
            List<BibEntryAiIdentifier> entriesFilter
    ) {
        // Look at this!
        return entriesFilter
                .stream()
                .flatMap(entryIdentifier ->
                        entryIdentifier
                                .entry()
                                .getFiles()
                                .stream()
                                .map(linkedFile ->
                                        linkedFile
                                                .findIn(entryIdentifier.databaseContext(), filePreferences)
                                                .flatMap(p -> universalContentParser.parse(longTaskInfo, p))
                                                .map(c -> new RelevantInformation(List.of(linkedFile.getLink()), c))
                                )
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                )
                .toList();
    }

    @Override
    public AnswerEngineKind getKind() {
        return AnswerEngineKind.FULL_DOCUMENT;
    }
}
