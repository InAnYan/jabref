package org.jabref.gui.ai.summary;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.ingestion.logic.parsing.UniversalContentParser;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTaskRequest;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.ResolvedBibEntryAiIdentifier;
import org.jabref.model.ai.summarization.BibEntrySummary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiSummaryViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        NO_DATABASE_PATH,
        NO_CITATION_KEY,
        WRONG_CITATION_KEY,
        NO_FILES,
        NO_SUPPORTED_FILE_TYPES,
        PROCESSING,
        DONE,
        ERROR_WHILE_GENERATING,
        READY,
        CANCELLED
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AiSummaryViewModel.class);

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.AI_TURNED_OFF);
    private final ObjectProperty<Exception> error = new SimpleObjectProperty<>(null);
    private final ObjectProperty<BibEntrySummary> summary = new SimpleObjectProperty<>();

    private final ObjectProperty<BibEntryAiIdentifier> entry = new SimpleObjectProperty<>();
    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ObjectProperty<Summarizator> summarizator = new SimpleObjectProperty<>();

    private final ObjectProperty<GenerateSummaryTask> currentTask = new SimpleObjectProperty<>();
    private final ChangeListener<TrackedBackgroundTask.Status> taskStateListener = (_, _, value) -> updateByTaskState(value);

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final ChatModel defaultChatModel;
    private final Summarizator defaultSummarizator;
    private final SummariesRepository summariesRepository;
    private final SummarizationTaskAggregator summarizationTaskAggregator;
    private final DialogService dialogService;

    public AiSummaryViewModel(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            ChatModel defaultChatModel,
            Summarizator defaultSummarizator,
            SummariesRepository summariesRepository,
            SummarizationTaskAggregator summarizationTaskAggregator,
            DialogService dialogService
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.defaultChatModel = defaultChatModel;
        this.defaultSummarizator = defaultSummarizator;
        this.summariesRepository = summariesRepository;
        this.summarizationTaskAggregator = summarizationTaskAggregator;
        this.dialogService = dialogService;

        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        BindingsHelper.bindEnum(
                state,
                State.AI_TURNED_OFF, aiPreferences.enableAiProperty().not(),
                State.NO_DATABASE_PATH, entry.map(BibEntryAiIdentifier::databaseContext).map(BibDatabaseContext::getDatabasePath).map(Optional::isEmpty),
                State.NO_CITATION_KEY, entry.map(BibEntryAiIdentifier::entry).map(BibEntry::getCitationKey).map(Optional::isEmpty),
                State.WRONG_CITATION_KEY, entry.map(CitationKeyCheck::citationKeyIsUnique).map(isUnique -> !isUnique),
                State.NO_FILES, entry.map(BibEntryAiIdentifier::entry).map(BibEntry::getFiles).map(List::isEmpty),
                State.NO_SUPPORTED_FILE_TYPES, entry.map(BibEntryAiIdentifier::entry).map(BibEntry::getFiles).map(l -> l.stream().map(f -> Path.of(f.getLink())).noneMatch(UniversalContentParser::isSupportedFileType)),
                State.DONE, summary.isNotNull(),
                State.PROCESSING, currentTask.isNotNull(),
                State.ERROR_WHILE_GENERATING, error.isNotNull(),
                State.CANCELLED, currentTask.map(TrackedBackgroundTask::getStatus).map(s -> s == TrackedBackgroundTask.Status.CANCELLED).orElse(false),
                State.READY
        );

        BindingsHelper.bindInternalListener(
                currentTask,
                GenerateSummaryTask::statusProperty,
                taskStateListener
        );
    }

    private void setupListeners() {
        ListenersHelper.onChangeNonNull(
                entry,
                this::prepareForEntry
        );

        ListenersHelper.onChangeNonNullWhen(
                entry,
                state.isEqualTo(State.READY),
                this::processEntry
        );
    }

    private void setDefaultModels() {
        chatModel.set(defaultChatModel);
        summarizator.set(defaultSummarizator);
    }

    private void clearTask() {
        if (currentTask.get() != null) {
            currentTask.set(null);
        }
    }

    public void regenerate() {
        regenerate(getEntry());
    }

    public void regenerateCustom() {
        regenerateCustom(getEntry());
    }

    public void generate() {
        generate(getEntry());
    }

    public void cancel() {
        if (currentTask.get() != null) {
            currentTask.get().cancel();
        }
    }

    private void prepareForEntry() {
        clearTask();
        summary.set(null);
        error.set(null);
    }

    private void processEntry(BibEntryAiIdentifier identifier) {
        BibDatabaseContext bibDatabaseContext = identifier.databaseContext();
        BibEntry entry = identifier.entry();

        assert bibDatabaseContext.getDatabasePath().isPresent();
        assert entry.getCitationKey().isPresent();

        ResolvedBibEntryAiIdentifier identifier2 = new ResolvedBibEntryAiIdentifier(
                bibDatabaseContext.getDatabasePath().get(),
                entry.getCitationKey().get()
        );

        Optional<BibEntrySummary> summary = summariesRepository.get(identifier2);
        if (summary.isPresent()) {
            this.summary.set(summary.get());
        } else {
            Optional<GenerateSummaryTask> task = summarizationTaskAggregator.getTask(entry);
            if (task.isPresent()) {
                currentTask.set(task.get());
            } else {
                generate();
            }
        }
    }

    private void regenerate(BibEntryAiIdentifier identifier) {
        clearSummary(identifier);
        generate(identifier);
    }

    private void regenerateCustom(BibEntryAiIdentifier identifier) {
        if (identifier == null) {
            return;
        }

        AiSummaryParametersDialog parametersDialog = new AiSummaryParametersDialog();
        Optional<Summarizator> customSummarizator = dialogService.showCustomDialogAndWait(parametersDialog);

        if (customSummarizator.isEmpty()) {
            return;
        }

        summarizator.set(customSummarizator.get());

        clearSummary(identifier);
        startSummarization(identifier);
    }

    private void generate(BibEntryAiIdentifier identifier) {
        setDefaultModels();
        clearSummary(identifier);
        startSummarization(identifier);
    }

    public void clearSummary(BibEntryAiIdentifier identifier) {
        if (identifier == null) {
            return;
        }

        Optional<Path> path = identifier.databaseContext().getDatabasePath();
        Optional<String> citationKey = identifier.entry().getCitationKey();

        if (path.isEmpty() || citationKey.isEmpty()) {
            LOGGER.warn("Could not clear stored summary for regeneration");
            return;
        }

        summariesRepository.clear(new ResolvedBibEntryAiIdentifier(path.get(), citationKey.get()));

        summary.set(null);
    }

    private void startSummarization(BibEntryAiIdentifier identifier) {
        if (identifier == null) {
            return;
        }

        BibDatabaseContext bibDatabaseContext = identifier.databaseContext();
        BibEntry entry = identifier.entry();

        GenerateSummaryTask task = summarizationTaskAggregator.start(
                new GenerateSummaryTaskRequest(
                        filePreferences,
                        chatModel.get(),
                        summariesRepository,
                        summarizator.get(),
                        bibDatabaseContext,
                        entry,
                        true
                )
        );

        currentTask.set(task);
    }

    private void updateByTaskState(TrackedBackgroundTask.Status value) {
        if (currentTask.get() == null) {
            return;
        }

        UiTaskExecutor.runInJavaFXThread(() -> {
            switch (value) {
                case TrackedBackgroundTask.Status.ERROR -> {
                    error.set(currentTask.get().getException());
                }

                case TrackedBackgroundTask.Status.SUCCESS -> {
                    summary.set(currentTask.get().getResult());
                }
            }
        });
    }

    public ObjectProperty<BibEntryAiIdentifier> entryProperty() {
        return entry;
    }

    public BibEntryAiIdentifier getEntry() {
        return entry.get();
    }

    public void setEntry(BibEntryAiIdentifier entry) {
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

    public ObjectProperty<GenerateSummaryTask> currentTaskProperty() {
        return currentTask;
    }
}
