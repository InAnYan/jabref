package org.jabref.logic.ai.summarization.tasks.generatesummary;

import org.jabref.logic.ai.summarization.SummariesService;
import org.jabref.logic.ai.summarization.logic.PersistentBibEntrySummarizator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.summarization.BibEntrySummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task generates a new summary for an entry.
 * It will check if summary was already generated.
 * And it also will store the summary.
 * <p>
 * This task is created in the {@link SummariesService}, and stored then in a {@link SummariesRepository}.
 */
public class GenerateSummaryTask extends TrackedBackgroundTask<BibEntrySummary> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSummaryTask.class);

    private final GenerateSummaryTaskRequest request;
    private final String citationKey; // Useful for logging.
    private final PersistentBibEntrySummarizator persistentBibEntrySummarizator;

    public GenerateSummaryTask(GenerateSummaryTaskRequest request) {
        this.request = request;
        this.citationKey = request.entry().getCitationKey().orElse("<no citation key>");

        this.persistentBibEntrySummarizator = new PersistentBibEntrySummarizator(
                request.filePreferences(),
                request.summariesRepository(),
                request.summarizator()
        );

        configure();
    }

    private void configure() {
        showToUser(true);
        titleProperty().set(Localization.lang("Waiting summary for %0...", citationKey));
    }

    @Override
    public BibEntrySummary perform() throws InterruptedException {
        LOGGER.debug("Starting summarization task for entry {}", citationKey);

        LongTaskInfo longTaskInfo = new LongTaskInfo(
                progressCounter,
                request.shutdownSignal()
        );

        BibEntrySummary bibEntrySummary = persistentBibEntrySummarizator.summarize(
                request.chatModel(),
                longTaskInfo,
                request.bibDatabaseContext(),
                request.entry(),
                request.regenerate()
        );

        LOGGER.debug("Finished summarization task for entry {}", citationKey);
        progressCounter.stop();

        return bibEntrySummary;
    }
}
