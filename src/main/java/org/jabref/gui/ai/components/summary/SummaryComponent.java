package org.jabref.gui.ai.components.summary;

import java.nio.file.Path;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.privacynotice.AiPrivacyNoticeGuardedComponent;
import org.jabref.gui.ai.components.util.errorstate.ErrorStateComponent;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.summarization.GenerateSummaryWithStorageTask;
import org.jabref.logic.ai.summarization.Summary;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryComponent extends AiPrivacyNoticeGuardedComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryComponent.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final BibEntry entry;
    private final CitationKeyGenerator citationKeyGenerator;
    private final AiPreferences aiPreferences;
    private final TaskExecutor taskExecutor;

    private final GenerateSummaryWithStorageTask generateSummaryWithStorageTask;

    public SummaryComponent(
            BibDatabaseContext bibDatabaseContext,
            BibEntry entry,
            AiService aiService,
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            ExternalApplicationsPreferences externalApplicationsPreferences,
            CitationKeyPatternPreferences citationKeyPatternPreferences,
            DialogService dialogService,
            TaskExecutor taskExecutor
    ) {
        super(aiPreferences, externalApplicationsPreferences, dialogService);

        this.bibDatabaseContext = bibDatabaseContext;
        this.entry = entry;
        this.citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences);
        this.aiPreferences = aiPreferences;
        this.taskExecutor = taskExecutor;

        this.generateSummaryWithStorageTask = aiService.generateSummaryWithStorageTask(
                entry,
                bibDatabaseContext,
                aiPreferences,
                filePreferences
        );

        generateSummaryWithStorageTask.onFailure(ex -> {
            setContent((_) -> showErrorWhileSummarizing(ex));
        });

        generateSummaryWithStorageTask.onSuccess(summary -> {
            setContent((_) -> showSummary(summary));
        });

        generateSummaryWithStorageTask.onRunning(() -> {
            setContent((_) -> showSummarizationInProgress());
        });

        generateSummaryWithStorageTask.onFinished(() -> {
            generateSummaryWithStorageTask.setBypassGetting(false);
        });

        rebuildUi();
    }

    @Override
    protected Node showPrivacyPolicyGuardedContent() {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            return showErrorNoDatabasePath();
        } else if (entry.getFiles().isEmpty()) {
            return showErrorNoFiles();
        } else if (entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).noneMatch(FileUtil::isPDFFile)) {
            return showErrorNotPdfs();
        } else if (!CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, entry)) {
            return tryToGenerateCitationKeyThenBind(entry);
        } else {
            taskExecutor.execute(generateSummaryWithStorageTask);
            return showSummarizationInProgress();
        }
    }

    private Node showErrorNoDatabasePath() {
        return new ErrorStateComponent(
                Localization.lang("Unable to generate summary"),
                Localization.lang("The path of the current library is not set, but it is required for summarization")
        );
    }

    private Node showErrorNotPdfs() {
        return new ErrorStateComponent(
                Localization.lang("Unable to generate summary"),
                Localization.lang("Only PDF files are supported.")
        );
    }

    private Node showErrorNoFiles() {
        return new ErrorStateComponent(
                Localization.lang("Unable to generate summary"),
                Localization.lang("Please attach at least one PDF file to enable summarization of PDF file(s).")
        );
    }

    private Node tryToGenerateCitationKeyThenBind(BibEntry entry) {
        if (citationKeyGenerator.generateAndSetKey(entry).isEmpty()) {
            return new ErrorStateComponent(
                    Localization.lang("Unable to generate summary"),
                    Localization.lang("Please provide a non-empty and unique citation key for this entry.")
            );
        } else {
            return showPrivacyPolicyGuardedContent();
        }
    }

    private Node showErrorWhileSummarizing(Exception ex) {
        LOGGER.error("Got an error while generating a summary for entry {}", entry.getCitationKey().orElse("<no citation key>"), ex);

        return ErrorStateComponent.withTextAreaAndButton(
                Localization.lang("Unable to chat"),
                Localization.lang("Got error while processing the file:"),
                ex.getLocalizedMessage(),
                Localization.lang("Regenerate"),
                () -> {
                    taskExecutor.execute(generateSummaryWithStorageTask);
                }
        );
    }

    private Node showSummarizationInProgress() {
        return ErrorStateComponent.withSpinner(
                Localization.lang("Processing..."),
                Localization.lang("The attached file(s) are currently being processed by %0. Once completed, you will be able to see the summary.", aiPreferences.getAiProvider().getLabel())
        );
    }

    private Node showSummary(Summary summary) {
        return new SummaryShowingComponent(summary, () -> {
            generateSummaryWithStorageTask.setBypassGetting(true);
            taskExecutor.execute(generateSummaryWithStorageTask);
        });
    }
}
