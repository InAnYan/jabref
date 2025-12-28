package org.jabref.gui.ai.summary;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.ingestion.logic.parsing.UniversalContentParser;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTaskRequest;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.summarization.BibEntrySummary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiSummaryViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        DONE,
        NO_DATABASE_PATH,
        NO_CITATION_KEY,
        WRONG_CITATION_KEY,
        NO_FILES,
        NO_SUPPORTED_FILE_TYPES,
        PROCESSING,
        CANCELLED,
        ERROR_WHILE_GENERATING,
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AiSummaryViewModel.class);

    private final GuiPreferences preferences;
    private final AiService aiService;
    private final DialogService dialogService;

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.AI_TURNED_OFF);
    private final ObjectProperty<Exception> error = new SimpleObjectProperty<>(null);
    private final ObjectProperty<BibEntrySummary> summary = new SimpleObjectProperty<>();

    private final ObjectProperty<FullBibEntryAiIdentifier> entry = new SimpleObjectProperty<>();
    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ObjectProperty<Summarizator> summarizator = new SimpleObjectProperty<>();

    @Nullable
    private GenerateSummaryTask currentTask = null;
    private final ChangeListener<TrackedBackgroundTask.Status> taskStateListener = (_, _, value) -> updateByTaskState(value);

    public AiSummaryViewModel(
            GuiPreferences guiPreferences,
            AiService aiService,
            DialogService dialogService
    ) {
        this.preferences = guiPreferences;
        this.aiService = aiService;
        this.dialogService = dialogService;

        AiPreferences aiPreferences = preferences.getAiPreferences();

        aiPreferences.enableAiProperty().addListener((_, _, value) -> {
            if (value) {
                if (getEntry() != null) {
                    updateState(getEntry());
                }
            } else {
                state.set(State.AI_TURNED_OFF);
            }
        });

        entry.addListener((_, _, newEntry) -> {
            if (currentTask != null) {
                currentTask.cancel();
                removeTaskListener();
                currentTask = null;
            }

            if (newEntry != null) {
                setDefaultModels();
                updateState(newEntry);
            } else {
                state.set(State.AI_TURNED_OFF);
                summary.set(null);
                error.set(null);
            }
        });
    }

    private void setDefaultModels() {
        chatModel.set(aiService.getChattingFeature().getCurrentChatModel());
        summarizator.set(aiService.getSummarizationFeature().getCurrentSummarizator());
    }

    public void regenerate() {
        if (getEntry() != null) {
            regenerate(getEntry());
        }
    }

    public void regenerateCustom() {
        if (getEntry() != null) {
            regenerateCustom(getEntry());
        }
    }

    public void generate() {
        if (getEntry() != null) {
            generate(getEntry());
        }
    }

    public void cancel() {
        state.set(State.CANCELLED);
        if (currentTask != null) {
            currentTask.cancel();
        }
    }

    private void updateState(FullBibEntryAiIdentifier identifier) {
        BibDatabaseContext bibDatabaseContext = identifier.databaseContext();
        BibEntry entry = identifier.entry();

        if (!preferences.getAiPreferences().getEnableAi()) {
            state.set(State.AI_TURNED_OFF);
        } else if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            state.set(State.NO_DATABASE_PATH);
        } else if (entry.getCitationKey().isEmpty() || CitationKeyCheck.hasEmptyCitationKey(entry)) {
            state.set(State.NO_CITATION_KEY);
        } else if (!CitationKeyCheck.citationKeyIsUnique(bibDatabaseContext, entry.getCitationKey().get())) {
            state.set(State.WRONG_CITATION_KEY);
        } else if (entry.getFiles().isEmpty()) {
            state.set(State.NO_FILES);
        } else if (entry.getFiles().stream().map(f -> Path.of(f.getLink())).noneMatch(UniversalContentParser::isSupportedFileType)) {
            state.set(State.NO_SUPPORTED_FILE_TYPES);
        } else {
            BibEntryAiIdentifier identifier2 = new BibEntryAiIdentifier(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get());

            Optional<BibEntrySummary> summary = aiService.getSummarizationFeature().getSummariesRepository().get(identifier2);
            if (summary.isPresent()) {
                this.summary.set(summary.get());
                state.set(State.DONE);
            } else {
                Optional<GenerateSummaryTask> task = aiService.getSummarizationFeature().getTaskAggregator().getTask(entry);
                if (task.isPresent()) {
                    updateCurrentTask(task.get());
                    state.set(State.PROCESSING);
                } else {
                    generate();
                }
            }
        }
    }

    private void regenerate(FullBibEntryAiIdentifier identifier) {
        clearSummary(identifier);
        generate(identifier);
    }

    private void regenerateCustom(FullBibEntryAiIdentifier identifier) {
        AiSummaryParametersDialog parametersDialog = new AiSummaryParametersDialog();
        Optional<Summarizator> customSummarizator = dialogService.showCustomDialogAndWait(parametersDialog);

        if (customSummarizator.isEmpty()) {
            return;
        }

        summarizator.set(customSummarizator.get());

        clearSummary(identifier);
        startSummarization(identifier);
    }

    private void generate(FullBibEntryAiIdentifier identifier) {
        setDefaultModels();
        startSummarization(identifier);
    }

    public void clearSummary(FullBibEntryAiIdentifier identifier) {
        Optional<Path> path = identifier.databaseContext().getDatabasePath();
        Optional<String> citationKey = identifier.entry().getCitationKey();

        if (path.isEmpty() || citationKey.isEmpty()) {
            LOGGER.warn("Could not clear stored summary for regeneration");
            return;
        }

        aiService.getSummarizationFeature().getSummariesRepository().clear(new BibEntryAiIdentifier(path.get(), citationKey.get()));
    }

    private void startSummarization(FullBibEntryAiIdentifier identifier) {
        BibDatabaseContext bibDatabaseContext = identifier.databaseContext();
        BibEntry entry = identifier.entry();

        state.set(State.PROCESSING);

        GenerateSummaryTask task = aiService.getSummarizationFeature().getTaskAggregator().start(
                new GenerateSummaryTaskRequest(
                        preferences.getFilePreferences(),
                        chatModel.get(),
                        aiService.getSummarizationFeature().getSummariesRepository(),
                        summarizator.get(),
                        bibDatabaseContext,
                        entry,
                        true,
                        aiService.getShutdownSignal()
                )
        );
        updateCurrentTask(task);
    }

    private void updateCurrentTask(GenerateSummaryTask task) {
        removeTaskListener();
        currentTask = task;
        currentTask.statusProperty().addListener(taskStateListener);
    }

    private void removeTaskListener() {
        if (currentTask != null) {
            currentTask.statusProperty().removeListener(taskStateListener);
        }
    }

    private void updateByTaskState(TrackedBackgroundTask.Status value) {
        assert currentTask != null;

        UiTaskExecutor.runInJavaFXThread(() -> {
            switch (value) {
                case TrackedBackgroundTask.Status.CANCELLED ->
                        state.set(State.CANCELLED);

                case TrackedBackgroundTask.Status.PENDING ->
                        state.set(State.PROCESSING);

                case TrackedBackgroundTask.Status.ERROR -> {
                    error.set(currentTask.getException());
                    state.set(State.ERROR_WHILE_GENERATING);
                }

                case TrackedBackgroundTask.Status.SUCCESS -> {
                    summary.set(currentTask.getResult());
                    state.set(State.DONE);
                }
            }
        });
    }

    public ObjectProperty<FullBibEntryAiIdentifier> entryProperty() {
        return entry;
    }

    public FullBibEntryAiIdentifier getEntry() {
        return entry.get();
    }

    public void setEntry(FullBibEntryAiIdentifier entry) {
        this.entry.set(entry);
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public ObjectProperty<Exception> errorProperty() {
        return error;
    }

    public ObjectProperty<BibEntrySummary> summaryProperty() {
        return summary;
    }

    public ObjectProperty<Summarizator> summarizatorProperty() {
        return summarizator;
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return chatModel;
    }
}
