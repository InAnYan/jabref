package org.jabref.logic.ai.summarization.tasks;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.SummariesService;
import org.jabref.logic.ai.summarization.algorithms.PersistentBibEntrySummarizer;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.ai.summarization.Summary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task generates a new summary for an entry.
 * It will check if summary was already generated.
 * And it also will store the summary.
 * <p>
 * This task is created in the {@link SummariesService}, and stored then in a {@link SummariesRepository}.
 */
public class GenerateSummaryTask extends BackgroundTask<Summary> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSummaryTask.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final BibEntry entry;
    private final String citationKey;
    private final ReadOnlyBooleanProperty shutdownSignal;

    private final PersistentBibEntrySummarizer persistentBibEntrySummarizer;

    private final ProgressCounter progressCounter = new ProgressCounter();

    public GenerateSummaryTask(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            AiTemplatesService aiTemplatesService,
            ChatModel chatLanguageModel,
            SummariesRepository summariesRepository,
            BibDatabaseContext bibDatabaseContext,
            BibEntry entry,
            ReadOnlyBooleanProperty shutdownSignal
    ) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.entry = entry;
        this.citationKey = entry.getCitationKey().orElse("<no citation key>");
        this.shutdownSignal = shutdownSignal;

        this.persistentBibEntrySummarizer = new PersistentBibEntrySummarizer(
                aiPreferences,
                filePreferences,
                summariesRepository,
                aiTemplatesService,
                chatLanguageModel
        );

        configure();
    }

    private void configure() {
        showToUser(true);
        titleProperty().set(Localization.lang("Waiting summary for %0...", citationKey));

        progressCounter.listenToAllProperties(this::updateProgress);
    }

    @Override
    public Summary call() {
        LOGGER.debug("Starting summarization task for entry {}", citationKey);

        Summary summary = persistentBibEntrySummarizer.summarize(entry, bibDatabaseContext, progressCounter, shutdownSignal);

        LOGGER.debug("Finished summarization task for entry {}", citationKey);
        progressCounter.stop();

        return summary;
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage());
    }
}
