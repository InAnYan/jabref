package org.jabref.logic.ai.summarization.algorithms;

import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.model.ai.chatting.ChatModelInfo;
import org.jabref.model.ai.summarization.BibEntrySummary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentBibEntrySummarizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentBibEntrySummarizer.class);

    private final SummariesRepository summariesRepository;

    private final BibEntrySummarizer bibEntrySummarizer;

    public PersistentBibEntrySummarizer(
            FilePreferences filePreferences,
            SummariesRepository summariesRepository,
            SummarizationAlgorithm summarizationAlgorithm
    ) {
        this.summariesRepository = summariesRepository;

        this.bibEntrySummarizer = new BibEntrySummarizer(
                filePreferences,
                summarizationAlgorithm
        );
    }

    public BibEntrySummary summarize(
            ChatModelInfo chatModelInfo,
            LongTaskInfo longTaskInfo,
            BibDatabaseContext bibDatabaseContext,
            BibEntry entry
    ) {
        Optional<BibEntrySummary> savedSummary = Optional.empty();

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. BibEntrySummary will not be stored in the next sessions");
        } else if (entry.getCitationKey().isEmpty()) {
            LOGGER.info("No citation key is present. BibEntrySummary will not be stored in the next sessions");
        } else {
            savedSummary = summariesRepository.get(
                    bibDatabaseContext.getDatabasePath().get(),
                    entry.getCitationKey().get()
            );
        }

        BibEntrySummary bibEntrySummary;

        if (savedSummary.isPresent()) {
            bibEntrySummary = savedSummary.get();
        } else {
            try {
                bibEntrySummary = bibEntrySummarizer.summarize(
                        chatModelInfo,
                        longTaskInfo,
                        bibDatabaseContext,
                        entry
                );
            } catch (InterruptedException e) {
                LOGGER.debug("There was a summarization task for {}. It will be canceled, because user quits JabRef.", entry.getCitationKey().orElse("<no citation key>"));
                return null;
            }
        }

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. BibEntrySummary will not be stored in the next sessions");
        } else if (CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, entry)) {
            LOGGER.info("No valid citation key is present. BibEntrySummary will not be stored in the next sessions");
        } else {
            summariesRepository.set(
                    bibDatabaseContext.getDatabasePath().get(),
                    entry.getCitationKey().get(),
                    bibEntrySummary
            );
        }

        return bibEntrySummary;
    }
}
