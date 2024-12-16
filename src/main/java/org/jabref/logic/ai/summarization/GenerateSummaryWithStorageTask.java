package org.jabref.logic.ai.summarization;

import java.util.Optional;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.templates.TemplatesService;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background task for 1) getting already generated summary of {@link org.jabref.model.entry.BibEntry},
 * 2) generating summary if there is no stored one, 3) storing summary in {@link SummariesStorage}.
 */
public class GenerateSummaryWithStorageTask extends GenerateSummaryTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSummaryWithStorageTask.class);

    private final BibEntry entry;
    private final BibDatabaseContext bibDatabaseContext;
    private final SummariesStorage summariesStorage;

    // Useful if you want to regenerate the summary.
    private boolean bypassGetting = false;

    public GenerateSummaryWithStorageTask(
            BibEntry entry,
            BibDatabaseContext bibDatabaseContext,
            SummariesStorage summariesStorage,
            ChatLanguageModel chatLanguageModel,
            TemplatesService templatesService,
            ReadOnlyBooleanProperty shutdownSignal,
            AiPreferences aiPreferences,
            FilePreferences filePreferences
    ) {
        super(
                entry,
                bibDatabaseContext,
                chatLanguageModel,
                templatesService,
                shutdownSignal,
                aiPreferences,
                filePreferences
        );

        this.entry = entry;
        this.bibDatabaseContext = bibDatabaseContext;
        this.summariesStorage = summariesStorage;
    }

    public void setBypassGetting(boolean bypassGetting) {
        this.bypassGetting = bypassGetting;
    }

    @Override
    public Summary call() throws Exception {
        if (!bypassGetting) {
            Optional<Summary> savedSummary = getStoredSummary();

            if (savedSummary.isPresent()) {
                return savedSummary.get();
            }
        }

        Summary summary = super.call();

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. Summary will not be stored in the next sessions");
        } else if (!CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, entry)) {
            LOGGER.info("No valid citation key is present. Summary will not be stored in the next sessions");
        } else {
            assert entry.getCitationKey().isPresent(); // Must be already checked by `CitationKeyCheck`.
            summariesStorage.set(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get(), summary);
        }

        return summary;
    }

    private Optional<Summary> getStoredSummary() {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. Summary will not be stored in the next sessions");
        } else if (entry.getCitationKey().isEmpty()) {
            LOGGER.info("No citation key is present. Summary will not be stored in the next sessions");
        } else {
            return summariesStorage.get(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get());
        }

        return Optional.empty();
    }
}
