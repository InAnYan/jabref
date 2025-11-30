package org.jabref.gui.entryeditor.aisummary;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.BibEntrySummary;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiSummaryViewModel extends AbstractViewModel {
    public enum State {
        PENDING,
        PROCESSING,
        DONE,
        ERROR_WHILE_GENERATING,
        NO_DATABASE_PATH,
        NO_CITATION_KEY,
        WRONG_CITATION_KEY,
        NO_FILES,
        NO_SUPPORTED_FILE_TYPES
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
    private final OptionalObjectProperty<Exception> error = new OptionalObjectProperty<>(Optional.empty());

    // Processing state properties.
    private final ObjectProperty<AiProvider> processingAiProvider = new SimpleObjectProperty<>();
    private final StringProperty processingLlmName = new SimpleStringProperty("");

    // Done state properties.
    private final StringProperty summaryContent = new SimpleStringProperty("");
    private final BooleanProperty summaryRenderMarkdown = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalDateTime> summaryTimestamp = new SimpleObjectProperty<>();
    private final ObjectProperty<AiProvider> summaryAiProvider = new SimpleObjectProperty<>();
    private final StringProperty summaryModel = new SimpleStringProperty("");

    private final ObjectProperty<Summarizator> summarizator = new SimpleObjectProperty<>();
    // Future proofing: in case it would be possible to change the chat model in the View, this property will be useful.
    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();

    private final ObjectProperty<FullBibEntryAiIdentifier> entry = new SimpleObjectProperty<>();

    public AiSummaryViewModel(
            GuiPreferences guiPreferences,
            AiService aiService
    ) {
        this.preferences = guiPreferences;
        this.aiService = aiService;

        AiPreferences aiPreferences = preferences.getAiPreferences();

        selectedSummarizatorKind.set(aiPreferences.getSummarizatorKind());

        showAiPrivacyPolicyGuard.bind(aiPreferences.enableAiProperty());
        aiPreferences.aiProviderProperty().bind(processingAiProvider);
        aiPreferences.addListenerToChatModels(() -> processingLlmName.set(aiPreferences.getSelectedChatModel()));

        selectedSummarizatorKind.addListener((_, _, newValue) ->
                summarizator.set(SummarizatorFactory.createSummarizator(aiService.getCurrentAiTemplates(), newValue)));

        chatModel.set(aiService.getChatLanguageModel());

        entry.addListener((_, _, newEntry) -> {
                    if (aiPreferences.getEnableAi()) {
                        generate(newEntry);
                    }
                }
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

    private void regenerate(FullBibEntryAiIdentifier identifier) {
        clearSummary(identifier);
        startSummarization(identifier, true);
    }

    private void generate(FullBibEntryAiIdentifier identifier) {
        startSummarization(identifier, false);
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

    private void startSummarization(FullBibEntryAiIdentifier identifier, boolean regenerate) {
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
        }

        GenerateSummaryTask task = aiService.getSummarizationTaskAggregator().start(
                new GenerateSummaryTaskRequest(
                        preferences.getFilePreferences(),
                        chatModel.get(),
                        aiService.getSummariesRepository(),
                        summarizator.get(),
                        bibDatabaseContext,
                        entry,
                        regenerate,
                        aiService.getShutdownSignal()
                )
        );

        task.statusProperty().addListener((_, _, value) -> {
            switch (value) {
                case TrackedBackgroundTask.Status.CANCELLED ->
                        state.set(State.PENDING);

                case TrackedBackgroundTask.Status.PENDING ->
                        state.set(State.PROCESSING);

                case TrackedBackgroundTask.Status.ERROR -> {
                    state.set(State.ERROR_WHILE_GENERATING);
                    error.set(Optional.of(task.getException()));
                }

                case TrackedBackgroundTask.Status.SUCCESS -> {
                    state.set(State.DONE);
                    BibEntrySummary summary = task.getResult();
                    summaryContent.set(summary.content());
                    summaryAiProvider.set(summary.aiProvider());
                    summaryModel.set(summary.model());
                    summaryTimestamp.set(summary.timestamp());
                }
            }
        });
    }

    public BooleanProperty showAiPrivacyPolicyGuardProperty() {
        return showAiPrivacyPolicyGuard;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public OptionalObjectProperty<Exception> errorProperty() {
        return error;
    }

    public ObjectProperty<AiProvider> processingAiProviderProperty() {
        return processingAiProvider;
    }

    public StringProperty processingLlmNameProperty() {
        return processingLlmName;
    }

    public StringProperty summaryContentProperty() {
        return summaryContent;
    }

    public BooleanProperty summaryRenderMarkdownProperty() {
        return summaryRenderMarkdown;
    }

    public ObjectProperty<LocalDateTime> summaryTimestampProperty() {
        return summaryTimestamp;
    }

    public ObjectProperty<AiProvider> summaryAiProviderProperty() {
        return summaryAiProvider;
    }

    public StringProperty summaryModelProperty() {
        return summaryModel;
    }

    public ListProperty<SummarizatorKind> summarizatorKindsProperty() {
        return summarizatorKinds;
    }

    public ObjectProperty<SummarizatorKind> selectedSummarizatorKindProperty() {
        return selectedSummarizatorKind;
    }
}
