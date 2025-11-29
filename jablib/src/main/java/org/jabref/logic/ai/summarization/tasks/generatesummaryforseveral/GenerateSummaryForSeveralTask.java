package org.jabref.logic.ai.summarization.tasks.generatesummaryforseveral;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javafx.util.Pair;

import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTask;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task generates summaries for several {@link BibEntry}ies (typically used for groups).
 * It will check if summaries were already generated.
 * And it also will store the summaries.
 */
public class GenerateSummaryForSeveralTask extends TrackedBackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSummaryForSeveralTask.class);

    private final GenerateSummaryForSeveralTaskRequest request;

    public GenerateSummaryForSeveralTask(GenerateSummaryForSeveralTaskRequest request) {
        this.request = request;

        configure();
    }

    private void configure() {
        showToUser(true);
        titleProperty().set(Localization.lang("Generating summaries for %0", request.groupName().get()));
        request.groupName().addListener((_, _, newValue) -> titleProperty().set(Localization.lang("Generating summaries for %0", newValue)));

        progressCounter.increaseWorkMax(request.entries().size());
    }

    @Override
    public Void perform() throws ExecutionException, InterruptedException {
        LOGGER.debug("Starting summaries generation of several files for {}", request.groupName().get());

        // We don't really care about the types. We just need to wait here.
        List<Future<?>> futures = new ArrayList<>();

        request.entries()
               .stream()
               .map(entry -> {
                   Pair<? extends Future<?>, GenerateSummaryTask> pair = request.summarizationTaskAggregator().startWithFuture(
                           request.toSingle(entry)
                   );

                   pair.getValue().statusProperty().addListener((_, _, newValue) -> {
                       if (newValue.isFinished()) {
                           progressCounter.increaseWorkDone(1);
                       }
                   });

                   return pair.getKey();
               })
               .forEach(futures::add);

        for (Future<?> future : futures) {
            future.get();
        }

        LOGGER.debug("Finished summary generation task of several files for {}", request.groupName().get());
        progressCounter.stop();
        return null;
    }
}
