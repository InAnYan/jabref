package org.jabref.logic.ai.summarization.logic;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.summarization.BibEntrySummary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Will clear existing summary.
///
/// Other things check if summary already exists.
public class PersistentBibEntrySummarizator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentBibEntrySummarizator.class);

    private final SummariesRepository summariesRepository;

    private final BibEntrySummarizator bibEntrySummarizator;

    public PersistentBibEntrySummarizator(
            FilePreferences filePreferences,
            SummariesRepository summariesRepository,
            Summarizator summarizator
    ) {
        this.summariesRepository = summariesRepository;

        this.bibEntrySummarizator = new BibEntrySummarizator(
                filePreferences,
                summarizator
        );
    }

    public BibEntrySummary summarize(
            ChatModel chatModel,
            LongTaskInfo longTaskInfo,
            BibDatabaseContext bibDatabaseContext,
            BibEntry entry
    ) throws InterruptedException {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. BibEntrySummary will not be stored in the next sessions");
        } else if (entry.getCitationKey().isEmpty()) {
            LOGGER.info("No citation key is present. BibEntrySummary will not be stored in the next sessions");
        }

        BibEntrySummary bibEntrySummary;

        try {
            bibEntrySummary = bibEntrySummarizator.summarize(
                    chatModel,
                    longTaskInfo,
                    bibDatabaseContext,
                    entry
            );
        } catch (InterruptedException e) {
            LOGGER.debug("There was a summarization task for {}. It will be canceled, because user quits JabRef.", entry.getCitationKey().orElse("<no citation key>"));
            throw e;
        }

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. BibEntrySummary will not be stored in the next sessions");
        } else if (!CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, entry)) {
            LOGGER.info("No valid citation key is present. Summary will not be stored in the next sessions");
        } else {
            summariesRepository.set(
                    new BibEntryAiIdentifier(
                            bibDatabaseContext.getDatabasePath().get(),
                            entry.getCitationKey().get()
                    ),
                    bibEntrySummary
            );
        }

        return bibEntrySummary;
    }
}
