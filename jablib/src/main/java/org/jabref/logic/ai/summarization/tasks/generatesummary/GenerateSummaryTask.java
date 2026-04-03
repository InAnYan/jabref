package org.jabref.logic.ai.summarization.tasks.generatesummary;

import org.jabref.logic.ai.summarization.logic.PersistentBibEntrySummarizator;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.summarization.AiSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateSummaryTask extends TrackedBackgroundTask<AiSummary> {
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
    public AiSummary perform() throws InterruptedException {
        LOGGER.debug("Starting summarization task for entry {}", citationKey);

        AiSummary aiSummary = persistentBibEntrySummarizator.summarize(
                request.chatModel(),
                request.bibDatabaseContext(),
                request.entry(),
                request.regenerate()
        );

        LOGGER.debug("Finished summarization task for entry {}", citationKey);
        progressCounter.stop();

        return aiSummary;
    }
}
