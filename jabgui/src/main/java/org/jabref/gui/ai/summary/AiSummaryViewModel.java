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
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.ChatModelFactory;
import org.jabref.logic.ai.ingestion.logic.parsing.UniversalContentParser;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.InMemorySummaryCache;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.logic.SummarizatorFactory;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTaskRequest;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiSummaryViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        NO_DATABASE_PATH,
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
    private final ObjectProperty<AiSummary> summary = new SimpleObjectProperty<>();

    private final ObjectProperty<FullBibEntry> entry = new SimpleObjectProperty<>();
    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ObjectProperty<Summarizator> summarizator = new SimpleObjectProperty<>();

    private final ObjectProperty<GenerateSummaryTask> currentTask = new SimpleObjectProperty<>();
    private final ChangeListener<TrackedBackgroundTask.Status> taskStateListener = (_, _, value) -> updateByTaskState(value);

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final SummariesRepository summariesRepository;
    private final InMemorySummaryCache inMemoryCache;
    private final SummarizationTaskAggregator summarizationTaskAggregator;
    private final DialogService dialogService;

    public AiSummaryViewModel(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            SummariesRepository summariesRepository,
            InMemorySummaryCache inMemoryCache,
            SummarizationTaskAggregator summarizationTaskAggregator,
            DialogService dialogService
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.summariesRepository = summariesRepository;
        this.inMemoryCache = inMemoryCache;
        this.summarizationTaskAggregator = summarizationTaskAggregator;
        this.dialogService = dialogService;

        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        BindingsHelper.bindEnum(
                state,
                State.AI_TURNED_OFF, aiPreferences.enableAiProperty().not(),
                State.NO_DATABASE_PATH, entry.map(FullBibEntry::databaseContext).map(BibDatabaseContext::getDatabasePath).map(Optional::isEmpty),
                State.NO_FILES, entry.map(FullBibEntry::entry).map(BibEntry::getFiles).map(List::isEmpty),
                State.NO_SUPPORTED_FILE_TYPES, entry.map(FullBibEntry::entry).map(BibEntry::getFiles).map(l -> l.stream().map(f -> Path.of(f.getLink())).noneMatch(UniversalContentParser::isSupportedFileType)),
                State.DONE, summary.isNotNull(),
                State.CANCELLED, currentTask.map(TrackedBackgroundTask::getStatus).map(s -> s == TrackedBackgroundTask.Status.CANCELLED).orElse(false),
                State.ERROR_WHILE_GENERATING, error.isNotNull(),
                State.PROCESSING, currentTask.isNotNull(),
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

        // Rebuild chat model when relevant preferences change (also calls immediately)
        BindingsHelper.subscribeToChanges(
                this::rebuildChatModel,
                aiPreferences.enableAiProperty(),
                aiPreferences.aiProviderProperty(),
                aiPreferences.customizeExpertSettingsProperty(),
                aiPreferences.temperatureProperty()
        );
        aiPreferences.addListenerToChatModels(this::rebuildChatModel);
        aiPreferences.addListenerToApiBaseUrls(this::rebuildChatModel);
        aiPreferences.setApiKeyChangeListener(this::rebuildChatModel);

        // Rebuild summarizator when relevant preferences change (also calls immediately)
        BindingsHelper.subscribeToChanges(
                this::rebuildSummarizator,
                aiPreferences.summarizatorKindProperty(),
                aiPreferences.summarizationChunkSystemMessageTemplateProperty(),
                aiPreferences.summarizationChunkUserMessageTemplateProperty(),
                aiPreferences.summarizationCombineSystemMessageTemplateProperty(),
                aiPreferences.summarizationCombineUserMessageTemplateProperty(),
                aiPreferences.summarizationFullDocumentSystemMessageTemplateProperty(),
                aiPreferences.summarizationFullDocumentUserMessageTemplateProperty()
        );
    }

    private void rebuildChatModel() {
        chatModel.set(ChatModelFactory.create(aiPreferences));
    }

    private void rebuildSummarizator() {
        summarizator.set(SummarizatorFactory.create(aiPreferences));
    }

    /**
     * Resets the chat model and summarizator to the default values from AI preferences.
     * Called before generating a summary to ensure default models are used
     * (as opposed to a custom summarizator set by {@link #regenerateCustom()}).
     */
    private void setDefaultModels() {
        rebuildChatModel();
        rebuildSummarizator();
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

    private void processEntry(FullBibEntry fullEntry) {
        // 1. Check persistent storage (requires valid citation key + AI library ID).
        Optional<AiSummary> persistedSummary = fullEntry.toAiSummaryIdentifier()
                                                        .flatMap(summariesRepository::get);
        if (persistedSummary.isPresent()) {
            this.summary.set(persistedSummary.get());
            return;
        }

        // 2. Check RAM cache (works for all entries, even without a citation key).
        Optional<AiSummary> cachedSummary = inMemoryCache.get(fullEntry.entry());
        if (cachedSummary.isPresent()) {
            this.summary.set(cachedSummary.get());
            return;
        }

        // 3. Reconnect to an in-progress task without starting a duplicate.
        Optional<GenerateSummaryTask> runningTask = summarizationTaskAggregator.getTask(fullEntry.entry());
        if (runningTask.isPresent()) {
            GenerateSummaryTask task = runningTask.get();
            currentTask.set(task);
            summarizator.set(task.getRequest().summarizator());
            chatModel.set(task.getRequest().chatModel());
            // Edge case: task finished in the narrow window between the aggregator lookup
            // and the bindInternalListener attaching the status listener. Handle immediately.
            switch (task.getStatus()) {
                case SUCCESS -> {
                    summary.set(task.getResult());
                    clearTask();
                }
                case ERROR -> {
                    error.set(task.getException());
                    clearTask();
                }
                default -> {
                }
            }
            return;
        }

        // 4. Nothing found — start a new generation task.
        generate();
    }

    private void regenerate(FullBibEntry identifier) {
        clearSummary(identifier);
        generate(identifier);
    }

    private void regenerateCustom(FullBibEntry identifier) {
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

    private void generate(FullBibEntry identifier) {
        setDefaultModels();
        clearSummary(identifier);
        startSummarization(identifier);
    }

    public void clearSummary(FullBibEntry fullEntry) {
        if (fullEntry == null) {
            return;
        }

        // Always clear from RAM cache (works regardless of citation key).
        inMemoryCache.remove(fullEntry.entry());

        // Try to clear from persistent storage (silently skipped if identifier is absent).
        fullEntry.toAiSummaryIdentifier()
                 .ifPresent(summariesRepository::clear);

        summary.set(null);
    }

    private void startSummarization(FullBibEntry fullEntry) {
        if (fullEntry == null) {
            return;
        }

        GenerateSummaryTask task = summarizationTaskAggregator.start(
                new GenerateSummaryTaskRequest(
                        filePreferences,
                        chatModel.get(),
                        summarizator.get(),
                        fullEntry,
                        true
                )
        );

        currentTask.set(task);
    }

    private void updateByTaskState(TrackedBackgroundTask.Status value) {
        // Capture task reference now (on background thread) before the FX-thread lambda runs,
        // as clearTask() may null it in the interim.
        GenerateSummaryTask task = currentTask.get();
        if (task == null) {
            return;
        }

        UiTaskExecutor.runInJavaFXThread(() -> {
            switch (value) {
                case TrackedBackgroundTask.Status.ERROR -> {
                    error.set(task.getException());
                    clearTask(); // detach listener, free the reference
                }
                case TrackedBackgroundTask.Status.SUCCESS -> {
                    summary.set(task.getResult());
                    clearTask(); // detach listener, free the reference
                }
            }
        });
    }

    public ObjectProperty<FullBibEntry> entryProperty() {
        return entry;
    }

    public FullBibEntry getEntry() {
        return entry.get();
    }

    public void setEntry(FullBibEntry entry) {
        this.entry.set(entry);
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public ObjectProperty<Exception> errorProperty() {
        return error;
    }

    public ObjectProperty<AiSummary> summaryProperty() {
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
