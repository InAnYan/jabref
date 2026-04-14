package org.jabref.gui.ai.chat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.AiChatJsonExporter;
import org.jabref.logic.ai.chatting.AiChatMarkdownExporter;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.ChatModelFactory;
import org.jabref.logic.ai.chatting.tasks.GenerateRagResponseTask;
import org.jabref.logic.ai.chatting.util.ChatHistoryUtils;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelFactory;
import org.jabref.logic.ai.followup.tasks.GenerateFollowUpQuestions;
import org.jabref.logic.ai.ingestion.DocumentSplitterFactory;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.google.common.collect.Comparators;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatViewModel.class);

    public enum State {
        AI_TURNED_OFF,
        NO_FILES,
        IDLE,
        WAITING_FOR_MESSAGE,
        ERROR
    }

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.IDLE);
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();
    private final ListProperty<FullBibEntry> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ObjectProperty<GenerateRagResponseTask> generateRagResponseTask = new SimpleObjectProperty<>();

    private final ListProperty<String> followUpQuestions = new SimpleListProperty<>(FXCollections.observableArrayList());
    private BackgroundTask<List<String>> generateFollowUpQuestionsTask;

    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private AsyncEmbeddingModel embeddingModel;
    private final ObjectProperty<DocumentSplitter> documentSplitter = new SimpleObjectProperty<>();

    private final TreeMap<List<FullBibEntry>, GenerateRagResponseTask> tasksMap =
            new TreeMap<>(Comparators.lexicographical(Comparator.comparing(id -> id.entry().getId())));

    private final TreeMap<List<FullBibEntry>, List<String>> followUpQuestionsCache =
            new TreeMap<>(Comparators.lexicographical(Comparator.comparing(id -> id.entry().getId())));

    private List<FullBibEntry> currentEntriesSnapshot = new ArrayList<>();

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;
    private final DialogService dialogService;
    private final IngestionTaskAggregator ingestionTaskAggregator;
    private final IngestedDocumentsRepository ingestedDocumentsRepository;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final TaskExecutor taskExecutor;

    private final ListProperty<ChatMessage> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty systemMessageTemplate = new SimpleStringProperty();
    private final StringProperty userMessageTemplate = new SimpleStringProperty();

    public AiChatViewModel(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            BibEntryTypesManager entryTypesManager,
            FieldPreferences fieldPreferences,
            IngestionTaskAggregator ingestionTaskAggregator,
            IngestedDocumentsRepository ingestedDocumentsRepository,
            DialogService dialogService,
            EmbeddingStore<TextSegment> embeddingStore,
            TaskExecutor taskExecutor
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.entryTypesManager = entryTypesManager;
        this.fieldPreferences = fieldPreferences;
        this.dialogService = dialogService;
        this.ingestionTaskAggregator = ingestionTaskAggregator;
        this.ingestedDocumentsRepository = ingestedDocumentsRepository;
        this.embeddingStore = embeddingStore;
        this.taskExecutor = taskExecutor;

        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        // [impl->req~ai.chat.customize-system-prompt~1]
        systemMessageTemplate.bind(aiPreferences.chattingSystemMessageTemplateProperty());
        userMessageTemplate.bind(aiPreferences.chattingUserMessageTemplateProperty());

        BooleanBinding isAiTurnedOff = aiPreferences.enableAiProperty().not();
        BooleanBinding isWaiting = generateRagResponseTask.isNotNull();
        BooleanBinding hasNoFiles = Bindings.createBooleanBinding(() ->
                        entries.get() == null ||
                                entries.isEmpty() ||
                                entries.stream().flatMap(identifier -> identifier.entry().getFiles().stream()).findAny().isEmpty(),
                entries
        );

        BooleanBinding isError = Bindings.createBooleanBinding(() -> {
            if (chatHistory.isEmpty()) {
                return false;
            }
            return chatHistory.getLast().role() == ChatMessage.Role.ERROR;
        }, chatHistory);

        BindingsHelper.bindEnum(
                state,
                State.AI_TURNED_OFF, isAiTurnedOff,
                State.WAITING_FOR_MESSAGE, isWaiting,
                State.NO_FILES, hasNoFiles,
                State.ERROR, isError,
                State.IDLE
        );
    }

    private void setupListeners() {
        BooleanBinding entriesPresent = entries.isNotNull().and(entries.emptyProperty().not());

        ListenersHelper.runWhenListChangesWithPrecondition(
                entries,
                aiPreferences.enableAiProperty().and(entriesPresent),
                this::changeEmbeddingTasks
        );

        // Listen to system message template changes and update chat history
        BindingsHelper.subscribeToChanges(
                () -> ChatHistoryUtils.updateSystemMessage(chatHistory, systemMessageTemplate.get()),
                systemMessageTemplate
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

        // Rebuild embedding model when relevant preferences change (also calls immediately)
        BindingsHelper.subscribeToChanges(
                this::rebuildEmbeddingModel,
                aiPreferences.enableAiProperty(),
                aiPreferences.customizeExpertSettingsProperty(),
                aiPreferences.embeddingModelProperty()
        );

        // Rebuild document splitter when relevant preferences change (also calls immediately)
        BindingsHelper.subscribeToChanges(
                this::rebuildDocumentSplitter,
                aiPreferences.customizeExpertSettingsProperty(),
                aiPreferences.documentSplitterKindProperty(),
                aiPreferences.documentSplitterChunkSizeProperty(),
                aiPreferences.documentSplitterOverlapSizeProperty()
        );
    }

    private void rebuildChatModel() {
        chatModel.set(ChatModelFactory.create(aiPreferences));
    }

    private void rebuildEmbeddingModel() {
        if (embeddingModel != null) {
            embeddingModel.close();
        }
        embeddingModel = EmbeddingModelFactory.create(aiPreferences, dialogService, taskExecutor);
    }

    private void rebuildDocumentSplitter() {
        documentSplitter.set(DocumentSplitterFactory.create(aiPreferences));
    }

    private void changeEmbeddingTasks() {
        if (!currentEntriesSnapshot.isEmpty()) {
            followUpQuestionsCache.put(new ArrayList<>(currentEntriesSnapshot), new ArrayList<>(followUpQuestions));
        }

        if (generateFollowUpQuestionsTask != null && !generateFollowUpQuestionsTask.isCancelled()) {
            generateFollowUpQuestionsTask.cancel();
            generateFollowUpQuestionsTask = null;
        }

        currentEntriesSnapshot = new ArrayList<>(entries);
        List<String> cached = followUpQuestionsCache.get(currentEntriesSnapshot);
        if (cached != null) {
            followUpQuestions.setAll(cached);
        } else {
            followUpQuestions.clear();
        }

        generateEmbeddingsTasks.clear();
        // It's okay to pass null.
        generateRagResponseTask.set(tasksMap.get(entries));

        entries.forEach(identifier ->
                identifier.entry().getFiles().forEach(file -> {
                            GenerateEmbeddingsTask task = ingestionTaskAggregator.start(
                                    new GenerateEmbeddingsTaskRequest(
                                            filePreferences,
                                            ingestedDocumentsRepository,
                                            embeddingStore,
                                            embeddingModel,
                                            documentSplitter.get(),
                                            identifier.databaseContext(),
                                            file
                                    )
                            );

                            generateEmbeddingsTasks.add(task);
                        }
                )
        );
    }

    // [impl->req~ai.chat.uses-answer-engine~1]
    // [impl->req~ai.chat.context-awareness~1]
    public void sendMessage(String userMessage) {
        assert state.get() == State.IDLE;

        if (StringUtil.isBlank(userMessage)) {
            return;
        }

        followUpQuestions.clear();
        clearGenerateRagResponseTask();

        ChatMessage userChatMessage = ChatMessage.userMessage(userMessage);
        chatHistory.add(userChatMessage);

        GenerateRagResponseTask task = new GenerateRagResponseTask(
                chatModel.get(),
                answerEngine.get(),
                List.copyOf(chatHistory), // TODO: Why?
                userMessage,
                entries.get(),
                systemMessageTemplate.get(),
                userMessageTemplate.get()
        );

        List<FullBibEntry> taskEntries = entries.get();

        final ObservableList<ChatMessage> originalChatHistory = chatHistory.get();

        task.onSuccess(aiMessage -> {
            originalChatHistory.add(aiMessage);

            if (aiPreferences.getGenerateFollowUpQuestions() && chatModel.get() != null) {
                scheduleFollowUpQuestionsGeneration(userMessage, aiMessage.content());
            }
        });

        // [impl->req~ai.chat.show-errors~1]
        task.onFailure(ex -> originalChatHistory.add(ChatMessage.errorMessage(ex)));

        task.onFinished(() -> {
            tasksMap.remove(taskEntries);
            if (generateRagResponseTask.get() == task) {
                generateRagResponseTask.set(null);
            }
        });

        task.executeWith(taskExecutor);
        generateRagResponseTask.set(task);
        tasksMap.put(taskEntries, task);
    }

    private void scheduleFollowUpQuestionsGeneration(String userMessage, String aiResponse) {
        ChatModel currentChatModel = chatModel.get();
        if (currentChatModel == null) {
            return;
        }

        if (generateFollowUpQuestionsTask != null && !generateFollowUpQuestionsTask.isCancelled()) {
            generateFollowUpQuestionsTask.cancel();
        }

        List<FullBibEntry> entriesSnapshot = new ArrayList<>(entries);

        final ObservableList<String> originalFollowUpQuestions = followUpQuestions.get();

        generateFollowUpQuestionsTask = new GenerateFollowUpQuestions(
                currentChatModel,
                aiPreferences,
                userMessage,
                aiResponse
        );

        generateFollowUpQuestionsTask
                .onSuccess(questions -> {
                    originalFollowUpQuestions.setAll(questions);
                    followUpQuestionsCache.put(entriesSnapshot, new ArrayList<>(questions));
                })
                .onFailure(ex -> LOGGER.warn("Failed to generate follow-up questions", ex))
                .executeWith(taskExecutor);
    }

    public void sendFollowUpMessage(String question) {
        followUpQuestions.clear();
        sendMessage(question);
    }

    // [impl->req~ai.chat.clear-history~1]
    public void clearChatHistory() {
        chatHistory.clear();
        followUpQuestions.clear();
        followUpQuestionsCache.remove(new ArrayList<>(entries));
    }

    private void clearGenerateRagResponseTask() {
        if (generateRagResponseTask.get() != null) {
            if (!generateRagResponseTask.get().isCancelled()) {
                generateRagResponseTask.get().cancel();
            }
            generateRagResponseTask.set(null);
        }
    }

    // [impl->req~ai.chat.cancel-generation~1]
    // [impl->req~ai.chat.cancel-error-state~1]
    public void cancel() {
        assert state.get() == State.WAITING_FOR_MESSAGE || state.get() == State.ERROR;

        if (state.get() == State.WAITING_FOR_MESSAGE) {
            clearGenerateRagResponseTask();
        } else if (state.get() == State.ERROR) {
            if (!chatHistory.isEmpty()) {
                chatHistory.removeLast();
            }
        }
        followUpQuestions.clear();
    }

    // [impl->req~ai.chat.delete-messages~1]
    public void delete(String id) {
        assert state.get() == State.IDLE;
        ChatHistoryUtils.delete(chatHistory, id);
    }

    // [impl->req~ai.chat.regenerate-response~1]
    public void regenerate(String id) {
        assert state.get() == State.ERROR || state.get() == State.IDLE;

        String contentToRegenerate = ChatHistoryUtils.regenerate(chatHistory, id);

        if (contentToRegenerate != null) {
            sendMessage(contentToRegenerate);
        }
    }

    // [impl->req~ai.chat.retry-error~1]
    public void regenerate() {
        if (!chatHistory.isEmpty()) {
            regenerate(chatHistory.getLast().id());
        }
    }

    public void exportMarkdown() {
        List<ChatMessage> messages = chatHistoryProperty().get();

        if (messages == null || messages.isEmpty()) {
            dialogService.notify(Localization.lang("No chat history available to export"));
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.MARKDOWN)
                .withDefaultExtension(StandardFileType.MARKDOWN)
                .withInitialDirectory(Path.of(System.getProperty("user.home")))
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(path -> {
                         try {
                             AiChatMarkdownExporter exporter = new AiChatMarkdownExporter(entryTypesManager, fieldPreferences, aiPreferences.getMarkdownChatExportTemplate());
                             String content = exporter.export(buildMetadata(), getBibEntriesFromFullEntries(), getDatabaseModeOrDefault(), messages);
                             Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             dialogService.notify(Localization.lang("Export operation finished successfully."));
                         } catch (IOException e) {
                             LOGGER.error("Problem occurred while writing the export file", e);
                             dialogService.showErrorDialogAndWait(Localization.lang("Problem occurred while writing the export file"), e);
                         }
                     });
    }

    public void exportJson() {
        List<ChatMessage> messages = chatHistoryProperty().get();

        if (messages == null || messages.isEmpty()) {
            dialogService.notify(Localization.lang("No chat history available to export"));
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.JSON)
                .withDefaultExtension(StandardFileType.JSON)
                .withInitialDirectory(Path.of(System.getProperty("user.home")))
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(path -> {
                         try {
                             AiChatJsonExporter exporter = new AiChatJsonExporter(entryTypesManager, fieldPreferences);
                             String content = exporter.export(buildMetadata(), getBibEntriesFromFullEntries(), getDatabaseModeOrDefault(), messages);
                             Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             dialogService.notify(Localization.lang("Export operation finished successfully."));
                         } catch (IOException e) {
                             LOGGER.error("Problem occurred while writing the export file", e);
                             dialogService.showErrorDialogAndWait(Localization.lang("Problem occurred while writing the export file"), e);
                         }
                     });
    }

    private AiMetadata buildMetadata() {
        ChatModel model = chatModel.get();
        if (model == null) {
            return new AiMetadata(null, "", Instant.now());
        }
        return new AiMetadata(model.getAiProvider(), model.getName(), Instant.now());
    }

    private List<BibEntry> getBibEntriesFromFullEntries() {
        return entries.stream()
                      .map(FullBibEntry::entry)
                      .toList();
    }

    private BibDatabaseMode getDatabaseModeOrDefault() {
        return entries.isEmpty()
               ? BibDatabaseMode.BIBTEX
               : entries.getFirst().databaseContext().getMode();
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return chatHistory;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    // [impl->req~ai.chat.model-visibility~1]
    public ObjectProperty<ChatModel> chatModelProperty() {
        return chatModel;
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return answerEngine;
    }

    // [impl->req~ai.chat.ingestion-status~1]
    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return generateEmbeddingsTasks;
    }

    public ListProperty<String> followUpQuestionsProperty() {
        return followUpQuestions;
    }
}
