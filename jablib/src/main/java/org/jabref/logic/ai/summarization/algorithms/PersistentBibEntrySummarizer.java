package org.jabref.logic.ai.summarization.algorithms;

import java.util.Optional;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.ai.summarization.Summary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentBibEntrySummarizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentBibEntrySummarizer.class);

    private final SummariesRepository summariesRepository;

    private final BibEntrySummarizer bibEntrySummarizer;

    public PersistentBibEntrySummarizer(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            SummariesRepository summariesRepository,
            AiTemplatesService aiTemplatesService,
            ChatModel chatModel
    ) {
        this.summariesRepository = summariesRepository;

        this.bibEntrySummarizer = new BibEntrySummarizer(
                aiPreferences,
                filePreferences,
                aiTemplatesService,
                chatModel
        );
    }

    public Summary summarize(BibEntry entry, BibDatabaseContext bibDatabaseContext, ProgressCounter progressCounter, ReadOnlyBooleanProperty shutdownSignal) {
        Optional<Summary> savedSummary = Optional.empty();

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. Summary will not be stored in the next sessions");
        } else if (entry.getCitationKey().isEmpty()) {
            LOGGER.info("No citation key is present. Summary will not be stored in the next sessions");
        } else {
            savedSummary = summariesRepository.get(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get());
        }

        Summary summary;

        if (savedSummary.isPresent()) {
            summary = savedSummary.get();
        } else {
            try {
                summary = bibEntrySummarizer.summarize(entry, bibDatabaseContext, progressCounter, shutdownSignal);
            } catch (InterruptedException e) {
                LOGGER.debug("There was a summarization task for {}. It will be canceled, because user quits JabRef.", entry.getCitationKey().orElse("<no citation key>"));
                return null;
            }
        }

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. Summary will not be stored in the next sessions");
        } else if (CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, entry)) {
            LOGGER.info("No valid citation key is present. Summary will not be stored in the next sessions");
        } else {
            summariesRepository.set(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get(), summary);
        }

        return summary;
    }
}
