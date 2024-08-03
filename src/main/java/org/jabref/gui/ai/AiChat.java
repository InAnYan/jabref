package org.jabref.gui.ai;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.ai.components.aichat.AiChatUnguardedComponent;
import org.jabref.gui.ai.components.apikeymissing.ApiKeyMissingComponent;
import org.jabref.gui.ai.components.errorstate.ErrorStateComponent;
import org.jabref.gui.ai.components.privacynotice.PrivacyNoticeComponent;
import org.jabref.gui.entryeditor.AiChatTab;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiChatLogic;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.GenerateEmbeddingsTask;
import org.jabref.logic.ai.chathistory.AiChatHistory;
import org.jabref.logic.ai.chathistory.BibDatabaseChatHistory;
import org.jabref.logic.ai.chathistory.InMemoryAiChatHistory;
import org.jabref.logic.ai.embeddings.FullyIngestedDocumentsTracker;
import org.jabref.logic.ai.models.EmbeddingModel;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChat extends Tab {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatTab.class);

    private final List<BibEntry> entries;
    private final LibraryTabContainer libraryTabContainer;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final CitationKeyGenerator citationKeyGenerator;
    private final AiService aiService;

    public AiChat(List<BibEntry> entries,
                    LibraryTabContainer libraryTabContainer,
                     DialogService dialogService,
                     PreferencesService preferencesService,
                     AiService aiService,
                     BibDatabaseContext bibDatabaseContext,
                     TaskExecutor taskExecutor) {
        this.entries = entries;
        this.libraryTabContainer = libraryTabContainer;
        this.dialogService = dialogService;
        this.filePreferences = preferencesService.getFilePreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();
        this.aiService = aiService;
        this.bibDatabaseContext = bibDatabaseContext;
        this.taskExecutor = taskExecutor;
        this.citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, preferencesService.getCitationKeyPatternPreferences());

        aiService.getEmbeddingsManager().registerListener(new AiChat.FileIngestedListener());
        aiService.getEmbeddingModel().registerListener(new AiChat.EmbeddingModelBuiltListener());
    }

    private void rebuildUI() {
        if (!aiService.getPreferences().getEnableAi()) {
            showPrivacyNotice();
        } else if (aiService.getPreferences().getApiToken().isEmpty()) {
            showApiKeyMissing();
        } else if (!aiService.getEmbeddingModel().isPresent()) {
            if (aiService.getEmbeddingModel().hadErrorWhileBuildingModel()) {
                showErrorWhileBuildingEmbeddingModel();
            } else {
                showBuildingEmbeddingModel();
            }
        } else if (!allCitationKeysAreValid()) {
            tryToGenerateCitationKeys();

            if (!allCitationKeysAreValid()) {
                showInvalidCitationKeys();
            } else {
                rebuildUI();
            }
        } else if (!allEntriesHaveFiles()) {
            showWarningNoFiles();
        } else if (!allEntriesArePdfs()) {
            showNotPdfs();
        } else if (!allFilesIngested) {
            startIngestingUningested();
            showUnderIngestion();
        } else {
            rebuildCorrectEntries();
        }
    }

    private void showPrivacyNotice() {
        setContent(new PrivacyNoticeComponent(dialogService, aiService.getPreferences(), filePreferences, this::rebuildUI));
    }

    private void showApiKeyMissing() {
        setContent(new ApiKeyMissingComponent(libraryTabContainer, dialogService));
    }

    private void showErrorWhileBuildingEmbeddingModel() {
        setContent(
                ErrorStateComponent.withTextAreaAndButton(
                        Localization.lang("Unable to chat"),
                        Localization.lang("An error occurred while building the embedding model"),
                        aiService.getEmbeddingModel().getErrorWhileBuildingModel(),
                        Localization.lang("Try to rebuild again"),
                        () -> aiService.getEmbeddingModel().startRebuildingTask()
                )
        );
    }

    public void showBuildingEmbeddingModel() {
        setContent(
                ErrorStateComponent.withSpinner(
                        Localization.lang("Please wait"),
                        Localization.lang("Embedding model is currently being downloaded. After the download is complete, you will be able to chat with your files")
                )
        );
    }

    private boolean allCitationKeysAreValid() {
        return entries.stream().allMatch(this::citationKeyIsValid);
    }

    private void tryToGenerateCitationKeys() {
        entries.forEach(citationKeyGenerator::generateAndSetKey);
    }

    private void showInvalidCitationKeys() {
        setContent(
                ErrorStateComponent.withBibEntries(
                        Localization.lang("Unable to chat"),
                        Localization.lang("Please provide a non-empty and unique citation key for this entry."),
                        getEntriesWithInvalidCitationKeys()
                )
        );
    }

    private List<BibEntry> getEntriesWithInvalidCitationKeys() {
        return entries.stream().filter(entry -> !citationKeyIsValid(entry)).toList();
    }

    private boolean allEntriesHaveFiles() {
        return entries.stream().noneMatch(entry -> entry.getFiles().isEmpty());
    }

    private void showErrorNotIngested() {
        setContent(
                ErrorStateComponent.withSpinner(
                        Localization.lang("Please wait"),
                        Localization.lang("The embeddings of the file are currently being generated. Please wait, and at the end you will be able to chat.")
                )
        );
    }

    private void showErrorNotPdfs() {
        setContent(
                new ErrorStateComponent(
                        Localization.lang("Unable to chat"),
                        Localization.lang("Only PDF files are supported.")
                )
        );
    }

    private void showErrorNoFiles() {
        setContent(
                new ErrorStateComponent(
                        Localization.lang("Unable to chat"),
                        Localization.lang("Please attach at least one PDF file to enable chatting with PDF files.")
                )
        );
    }

    private boolean citationKeyIsValid(BibEntry bibEntry) {
        return !hasEmptyCitationKey(bibEntry) && bibEntry.getCitationKey().map(this::citationKeyIsUnique).orElse(false);
    }

    private boolean hasEmptyCitationKey(BibEntry bibEntry) {
        return bibEntry.getCitationKey().map(String::isEmpty).orElse(true);
    }

    private boolean citationKeyIsUnique(String citationKey) {
        return bibDatabaseContext.getDatabase().getNumberOfCitationKeyOccurrences(citationKey) == 1;
    }

    private void startIngesting(BibEntry entry) {
        showErrorNotIngested();

        if (!entriesUnderIngestion.contains(entry)) {
            entriesUnderIngestion.add(entry);
            new GenerateEmbeddingsTask(entry.getFiles(), aiService.getEmbeddingsManager(), bibDatabaseContext, filePreferences, new SimpleBooleanProperty(false))
                    .onSuccess(res -> handleFocus())
                    .onFailure(this::showErrorWhileIngesting)
                    .executeWith(taskExecutor);
        }
    }

    private void showErrorWhileIngesting(Exception e) {
        setContent(ErrorStateComponent.withTextArea(Localization.lang("Unable to chat"), Localization.lang("Got error while processing the file:"), e.getMessage()));
        entriesUnderIngestion.remove(currentEntry);
        currentEntry.getFiles().stream().map(LinkedFile::getLink).forEach(link -> aiService.getEmbeddingsManager().removeDocument(link));
    }

    private void bindToCorrectEntry(BibEntry entry) {
        AiChatHistory aiChatHistory = getAiChatHistory(aiService, entry, bibDatabaseContext);
        AiChatLogic aiChatLogic = AiChatLogic.forBibEntry(aiService, aiChatHistory, entry);

        Node content = new AiChatUnguardedComponent(aiChatLogic, dialogService, taskExecutor);

        setContent(content);
    }

    private static AiChatHistory getAiChatHistory(AiService aiService, BibEntry entry, BibDatabaseContext bibDatabaseContext) {
        Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();

        if (databasePath.isEmpty() || entry.getCitationKey().isEmpty()) {
            LOGGER.warn("AI chat is constructed, but the database path is empty. Cannot store chat history");
            return new InMemoryAiChatHistory();
        } else if (entry.getCitationKey().isEmpty()) {
            LOGGER.warn("AI chat is constructed, but the entry citation key is empty. Cannot store chat history");
            return new InMemoryAiChatHistory();
        } else {
            BibDatabaseChatHistory bibDatabaseChatHistory = aiService.getChatHistoryManager().getChatHistoryForBibDatabase(databasePath.get());
            return bibDatabaseChatHistory.getChatHistoryForEntry(entry.getCitationKey().get());
        }
    }

    private class FileIngestedListener {
        @Subscribe
        public void listen(FullyIngestedDocumentsTracker.DocumentIngestedEvent event) {
            UiTaskExecutor.runInJavaFXThread(AiChatTab.this::handleFocus);
        }
    }

    private class EmbeddingModelBuiltListener {
        @Subscribe
        public void listen(EmbeddingModel.EmbeddingModelBuiltEvent event) {
            UiTaskExecutor.runInJavaFXThread(AiChatTab.this::handleFocus);
        }
    }
}
