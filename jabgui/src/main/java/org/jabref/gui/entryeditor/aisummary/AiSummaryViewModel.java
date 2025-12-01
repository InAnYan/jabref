package org.jabref.gui.entryeditor.aisummary;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.pipeline.logic.parsing.UniversalContentParser;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.SummarizatorFactory;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTaskRequest;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.BibEntrySummary;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * State flow:
 * 1. (external) Entry bound -> no database path, no citation key, wrong citation key, no files, no supported file types, pending.
 * 2. Pending -> Processing.
 * 3. Processing -> done, error while generating.
 */
public class AiSummaryViewModel extends AbstractViewModel {
    public enum State {
        NO_DATABASE_PATH,
        NO_CITATION_KEY,
        WRONG_CITATION_KEY,
        NO_FILES,
        NO_SUPPORTED_FILE_TYPES,
        PENDING,
        PROCESSING,
        DONE,
        ERROR_WHILE_GENERATING,
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AiSummaryViewModel.class);

    private final GuiPreferences preferences;
    private final AiService aiService;

    // Global properties.
    private final BooleanProperty showAiPrivacyPolicyGuard = new SimpleBooleanProperty(true);
    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.PENDING);

    // Pending state properties.
    private final ListProperty<SummarizatorKind> summarizatorKinds = new SimpleListProperty<>(
            FXCollections.observableArrayList(SummarizatorKind.values())
    );
    private final ObjectProperty<SummarizatorKind> selectedSummarizatorKind = new SimpleObjectProperty<>();

    // Error state properties.
    private final ObjectProperty<Exception> error = new SimpleObjectProperty<>(null);

    // Processing state properties.
    private final ObjectProperty<AiProvider> processingAiProvider = new SimpleObjectProperty<>();
    private final StringProperty processingLlmName = new SimpleStringProperty("");

    // Done state properties.
    private final ObjectProperty<BibEntrySummary> summary = new SimpleObjectProperty<>();

    private final ObjectProperty<Summarizator> summarizator = new SimpleObjectProperty<>();
    // Future proofing: in case it would be possible to change the chat model in the View, this property will be useful.
    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();

    private final ObjectProperty<FullBibEntryAiIdentifier> entry = new SimpleObjectProperty<>();

    @Nullable
    private GenerateSummaryTask currentTask = null;
    private final ChangeListener<TrackedBackgroundTask.Status> taskStateListener = (_, _, value) -> updateByTaskState(value);

    public AiSummaryViewModel(
            GuiPreferences guiPreferences,
            AiService aiService
    ) {
        this.preferences = guiPreferences;
        this.aiService = aiService;

        AiPreferences aiPreferences = preferences.getAiPreferences();

        selectedSummarizatorKind.set(aiPreferences.getSummarizatorKind());
        showAiPrivacyPolicyGuard.set(!aiPreferences.getEnableAi());
        processingAiProvider.set(aiPreferences.getAiProvider());
        processingLlmName.set(aiPreferences.getSelectedChatModel());
        selectedSummarizatorKind.set(aiPreferences.getSummarizatorKind());
        chatModel.set(aiService.getChatLanguageModel());

        showAiPrivacyPolicyGuard.bind(aiPreferences.enableAiProperty().not());
        processingAiProvider.bind(aiPreferences.aiProviderProperty());
        aiPreferences.addListenerToChatModels(() -> processingLlmName.set(aiPreferences.getSelectedChatModel()));

        selectedSummarizatorKind.addListener((_, _, newValue) ->
                summarizator.set(SummarizatorFactory.createSummarizator(aiService.getCurrentAiTemplates(), newValue)));
        summarizator.set(SummarizatorFactory.createSummarizator(aiService.getCurrentAiTemplates(), selectedSummarizatorKind.get()));

        entry.addListener((_, _, identifier) ->
                updateState(identifier)
        );
    }

    public void bindEntry(FullBibEntryAiIdentifier entry) {
        this.entry.set(entry);
    }

    public void regenerate() {
        if (entry.get() != null) {
            regenerate(entry.get());
        }
    }

    public void generate() {
        if (entry.get() != null) {
            generate(entry.get());
        }
    }

    public void cancel() {
        state.set(State.PENDING);
    }

    private void updateState(FullBibEntryAiIdentifier identifier) {
        BibDatabaseContext bibDatabaseContext = identifier.databaseContext();
        BibEntry entry = identifier.entry();

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
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

            Optional<BibEntrySummary> summary = aiService.getSummariesRepository().get(identifier2);
            if (summary.isPresent()) {
                this.summary.set(summary.get());
                state.set(State.DONE);
            } else {
                Optional<GenerateSummaryTask> task = aiService.getSummarizationTaskAggregator().getTask(entry);
                if (task.isPresent()) {
                    updateCurrentTask(task.get());
                    state.set(State.PROCESSING);
                } else {
                    state.set(State.PENDING);
                }
            }
        }
    }

    private void regenerate(FullBibEntryAiIdentifier identifier) {
        clearSummary(identifier);
        state.set(State.PENDING);
    }

    private void generate(FullBibEntryAiIdentifier identifier) {
        startSummarization(identifier);
    }

    public void clearSummary(FullBibEntryAiIdentifier identifier) {
        Optional<Path> path = identifier.databaseContext().getDatabasePath();
        Optional<String> citationKey = identifier.entry().getCitationKey();

        if (path.isEmpty() || citationKey.isEmpty()) {
            LOGGER.warn("Could not clear stored summary for regeneration");
            return;
        }

        aiService.getSummariesRepository().clear(new BibEntryAiIdentifier(path.get(), citationKey.get()));
    }

    private void startSummarization(FullBibEntryAiIdentifier identifier) {
        BibDatabaseContext bibDatabaseContext = identifier.databaseContext();
        BibEntry entry = identifier.entry();

        state.set(State.PROCESSING);

        GenerateSummaryTask task = aiService.getSummarizationTaskAggregator().start(
                new GenerateSummaryTaskRequest(
                        preferences.getFilePreferences(),
                        chatModel.get(),
                        aiService.getSummariesRepository(),
                        summarizator.get(),
                        bibDatabaseContext,
                        entry,
                        false,
                        aiService.getShutdownSignal()
                )
        );

        updateCurrentTask(task);
    }

    private void updateCurrentTask(GenerateSummaryTask task) {
        if (currentTask != null) {
            currentTask.statusProperty().removeListener(taskStateListener);
        }
        currentTask = task;
        currentTask.statusProperty().addListener(taskStateListener);
    }

    private void updateByTaskState(TrackedBackgroundTask.Status value) {
        assert currentTask != null;

        switch (value) {
            case TrackedBackgroundTask.Status.CANCELLED ->
                    state.set(State.PENDING);

            case TrackedBackgroundTask.Status.PENDING ->
                    state.set(State.PROCESSING);

            case TrackedBackgroundTask.Status.ERROR -> {
                state.set(State.ERROR_WHILE_GENERATING);
                error.set(currentTask.getException());
            }

            case TrackedBackgroundTask.Status.SUCCESS -> {
                state.set(State.DONE);
                summary.set(currentTask.getResult());
            }
        }
    }

    public BooleanProperty showAiPrivacyPolicyGuardProperty() {
        return showAiPrivacyPolicyGuard;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public ObjectProperty<Exception> errorProperty() {
        return error;
    }

    public ObjectProperty<AiProvider> processingAiProviderProperty() {
        return processingAiProvider;
    }

    public StringProperty processingLlmNameProperty() {
        return processingLlmName;
    }

    public ObjectProperty<BibEntrySummary> summaryProperty() {
        return summary;
    }

    public ListProperty<SummarizatorKind> summarizatorKindsProperty() {
        return summarizatorKinds;
    }

    public ObjectProperty<SummarizatorKind> selectedSummarizatorKindProperty() {
        return selectedSummarizatorKind;
    }
}
