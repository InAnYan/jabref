package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.jabref.gui.entryeditor.AiChatTab;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.logic.InternalPreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.UiCommand;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;
import com.dlsc.gemsfx.PowerPane;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class EndToEndJabGuiTest extends ApplicationTest {

    @TempDir
    protected Path tempDir;

    protected JabRefGUI jabRefGUI;
    protected Stage stage;
    protected GuiPreferences preferences;
    protected JabRefFrame mainFrame;
    protected AiPreferences aiPreferences;
    protected DialogService dialogService;
    protected ChatModel mockChatModel;
    protected MockedStatic<ChatModelFactory> mockedChatModelFactory;

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("glass.platform", "headless");
        System.setProperty("prism.order", "sw");
    }

    protected boolean enableAi() {
        return false;
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        Localization.setLanguage(Language.ENGLISH);
        CSLStyleLoader.loadInternalStyles();

        Injector.setModelOrService(BuildInfo.class, new BuildInfo());

        if (mockedChatModelFactory != null) {
            mockedChatModelFactory.close();
            mockedChatModelFactory = null;
        }

        if (enableAi()) {
            mockedChatModelFactory = Mockito.mockStatic(ChatModelFactory.class, Mockito.CALLS_REAL_METHODS);
            mockChatModel = mock(ChatModel.class);
            when(mockChatModel.getName()).thenReturn("mock-chat-model");
            when(mockChatModel.getAiProvider()).thenReturn(AiProvider.OPEN_AI);
            when(mockChatModel.getContextWindowSize()).thenReturn(4096);
            when(mockChatModel.chat(any(List.class))).thenAnswer(invocation -> {
                List<dev.langchain4j.data.message.ChatMessage> messages = invocation.getArgument(0);
                String lastText = "";
                if (!messages.isEmpty()) {
                    dev.langchain4j.data.message.ChatMessage last = messages.getLast();
                    if (last instanceof dev.langchain4j.data.message.UserMessage userMessage) {
                        lastText = userMessage.singleText();
                    }
                }
                ChatResponse response = mock(ChatResponse.class);
                when(response.aiMessage()).thenReturn(new AiMessage("AI: " + lastText));
                return response;
            });
            mockedChatModelFactory.when(() -> ChatModelFactory.create(any(AiPreferences.class)))
                                  .thenReturn(mockChatModel);
        }

        preferences = mock(GuiPreferences.class);
        Injector.setModelOrService(CliPreferences.class, preferences);
        Injector.setModelOrService(GuiPreferences.class, preferences);
        setupIsolatedPreferences(preferences);

        JabRefGUI.setup(List.of(new UiCommand.BlankWorkspace()), preferences);
        jabRefGUI = new JabRefGUI();
        jabRefGUI.start(stage);

        stage.setOnHiding(null);
        stage.setOnCloseRequest(null);

        DialogService realDialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService = Mockito.spy(realDialogService);
        Injector.setModelOrService(DialogService.class, dialogService);

        PowerPane powerPane = (PowerPane) stage.getScene().getRoot();
        mainFrame = (JabRefFrame) powerPane.getContent();
    }

    protected void setupIsolatedPreferences(GuiPreferences prefs) {
        JournalAbbreviationRepository abbreviationRepository = new JournalAbbreviationRepository();
        BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();
        LayoutFormatterPreferences layoutFormatterPreferences = LayoutFormatterPreferences.getDefault();
        PreviewPreferences previewPreferences = PreviewPreferences.getDefaultWithStyles(
                layoutFormatterPreferences,
                abbreviationRepository,
                entryTypesManager
        );

        aiPreferences = createAiPreferences(enableAi());
        when(prefs.getAiPreferences()).thenReturn(aiPreferences);
        when(prefs.getEntryEditorPreferences()).thenReturn(EntryEditorPreferences.getDefault());
        when(prefs.getInternalPreferences()).thenReturn(InternalPreferences.getDefault());
        when(prefs.getLibraryPreferences()).thenReturn(LibraryPreferences.getDefault());
        when(prefs.getWorkspacePreferences()).thenReturn(WorkspacePreferences.getDefault());
        when(prefs.getGuiPreferences()).thenReturn(CoreGuiPreferences.getDefault());
        when(prefs.getKeyBindingRepository()).thenReturn(new KeyBindingRepository(List.of(), List.of()));
        when(prefs.getCustomEntryTypesRepository()).thenReturn(entryTypesManager);
        when(prefs.getLayoutFormatterPreferences()).thenReturn(layoutFormatterPreferences);
        when(prefs.getPreviewPreferences()).thenReturn(previewPreferences);
        when(prefs.getSearchPreferences()).thenReturn(SearchPreferences.getDefault());

        RemotePreferences remotePreferences = RemotePreferences.getDefault();
        remotePreferences.enableRemoteServerProperty().set(false);
        when(prefs.getRemotePreferences()).thenReturn(remotePreferences);

        when(prefs.getLastFilesOpenedPreferences()).thenReturn(org.jabref.logic.preferences.LastFilesOpenedPreferences.getDefault());
        when(prefs.getBibEntryPreferences()).thenReturn(org.jabref.model.entry.BibEntryPreferences.getDefault());
        when(prefs.getFilePreferences()).thenReturn(org.jabref.logic.FilePreferences.getDefault());
        when(prefs.getFieldPreferences()).thenReturn(org.jabref.logic.bibtex.FieldPreferences.getDefault());
        when(prefs.getCitationKeyPatternPreferences()).thenReturn(org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences.getDefault());
        when(prefs.getCleanupPreferences()).thenReturn(org.jabref.logic.cleanup.CleanupPreferences.getDefault());
        when(prefs.getExportPreferences()).thenReturn(org.jabref.logic.exporter.ExportPreferences.getDefault());
        when(prefs.getImporterPreferences()).thenReturn(org.jabref.logic.importer.ImporterPreferences.getDefault());
        when(prefs.getImportFormatPreferences()).thenReturn(mock(org.jabref.logic.importer.ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(prefs.getXmpPreferences()).thenReturn(org.jabref.logic.xmp.XmpPreferences.getDefault());
        when(prefs.getNameFormatterPreferences()).thenReturn(org.jabref.logic.layout.format.NameFormatterPreferences.getDefault());
        when(prefs.getAbbreviationPreferences()).thenReturn(org.jabref.logic.journals.AbbreviationPreferences.getDefault());
        when(prefs.getAutoLinkPreferences()).thenReturn(org.jabref.logic.util.io.AutoLinkPreferences.getDefault());
        when(prefs.getDOIPreferences()).thenReturn(org.jabref.logic.preferences.DOIPreferences.getDefault());
        when(prefs.getOwnerPreferences()).thenReturn(org.jabref.logic.preferences.OwnerPreferences.getDefault());
        when(prefs.getTimestampPreferences()).thenReturn(org.jabref.logic.preferences.TimestampPreferences.getDefault());
        when(prefs.getProxyPreferences()).thenReturn(org.jabref.logic.net.ProxyPreferences.getDefault());
        when(prefs.getSSLPreferences()).thenReturn(org.jabref.logic.net.ssl.SSLPreferences.getDefault());
        when(prefs.getGrobidPreferences()).thenReturn(org.jabref.logic.importer.util.GrobidPreferences.getDefault());
        when(prefs.getOcrPreferences()).thenReturn(org.jabref.logic.ocr.OcrPreferences.getDefault());
        when(prefs.getOpenOfficePreferences(org.mockito.ArgumentMatchers.any())).thenReturn(org.jabref.logic.openoffice.OpenOfficePreferences.getDefault());
        when(prefs.getProtectedTermsPreferences()).thenReturn(org.jabref.logic.protectedterms.ProtectedTermsPreferences.getDefault());
        when(prefs.getPushToApplicationPreferences()).thenReturn(org.jabref.logic.push.PushToApplicationPreferences.getDefault());
        when(prefs.getGitPreferences()).thenReturn(org.jabref.logic.git.preferences.GitPreferences.getDefault());

        when(prefs.getMainTablePreferences()).thenReturn(org.jabref.gui.maintable.MainTablePreferences.getDefault());
        when(prefs.getMainTableColumnPreferences()).thenReturn(org.jabref.gui.maintable.ColumnPreferences.getDefault());
        when(prefs.getSearchDialogColumnPreferences()).thenReturn(org.jabref.gui.maintable.ColumnPreferences.getDefault());
        when(prefs.getNameDisplayPreferences()).thenReturn(org.jabref.gui.maintable.NameDisplayPreferences.getDefault());
        when(prefs.getSidePanePreferences()).thenReturn(org.jabref.gui.frame.SidePanePreferences.getDefault());
        when(prefs.getExternalApplicationsPreferences()).thenReturn(org.jabref.gui.frame.ExternalApplicationsPreferences.getDefault());
        when(prefs.getUnlinkedFilesDialogPreferences()).thenReturn(org.jabref.gui.externalfiles.UnlinkedFilesDialogPreferences.getDefault());
        when(prefs.getAutoCompletePreferences()).thenReturn(org.jabref.gui.autocompleter.AutoCompletePreferences.getDefault());
        when(prefs.getMergeDialogPreferences()).thenReturn(org.jabref.gui.mergeentries.MergeDialogPreferences.getDefault());
        when(prefs.getCopyToPreferences()).thenReturn(org.jabref.gui.edit.CopyToPreferences.getDefault());
        when(prefs.getGroupsPreferences()).thenReturn(org.jabref.gui.groups.GroupsPreferences.getDefault());
        when(prefs.getSpecialFieldsPreferences()).thenReturn(org.jabref.gui.specialfields.SpecialFieldsPreferences.getDefault());
        when(prefs.getMrDlibPreferences()).thenReturn(org.jabref.logic.importer.fetcher.MrDlibPreferences.getDefault());
        when(prefs.getDonationPreferences()).thenReturn(org.jabref.gui.preferences.DonationPreferences.getDefault());
        when(prefs.getNewEntryPreferences()).thenReturn(org.jabref.gui.newentry.NewEntryPreferences.getDefault());
    }

    public static AiPreferences createAiPreferences(boolean enabled) {
        AiPreferences defaults = AiPreferences.getDefault();
        return new AiPreferences(
                enabled,
                defaults.getAutoGenerateEmbeddings(),
                defaults.getAutoGenerateSummaries(),
                defaults.getAiProvider(),
                defaults.getOpenAiChatModel(),
                defaults.getMistralAiChatModel(),
                defaults.getGeminiChatModel(),
                defaults.getHuggingFaceChatModel(),
                defaults.getCustomizeExpertSettings(),
                defaults.getOpenAiApiBaseUrl(),
                defaults.getMistralAiApiBaseUrl(),
                defaults.getGeminiApiBaseUrl(),
                defaults.getHuggingFaceApiBaseUrl(),
                defaults.getSummarizatorKind(),
                defaults.getTokenEstimatorKind(),
                defaults.embeddingModelProperty().get(),
                defaults.temperatureProperty().get(),
                defaults.contextWindowSizeProperty().get(),
                defaults.getDocumentSplitterKind(),
                defaults.documentSplitterChunkSizeProperty().get(),
                defaults.documentSplitterOverlapSizeProperty().get(),
                AnswerEngineKind.FULL_DOCUMENT,
                defaults.ragMaxResultsCountProperty().get(),
                defaults.ragMinScoreProperty().get(),
                defaults.getChattingSystemMessageTemplate(),
                defaults.getChattingUserMessageTemplate(),
                defaults.getSummarizationChunkSystemMessageTemplate(),
                defaults.getSummarizationCombineSystemMessageTemplate(),
                defaults.getSummarizationFullDocumentSystemMessageTemplate(),
                defaults.getCitationParsingSystemMessageTemplate(),
                defaults.getMarkdownChatExportTemplate(),
                false,
                defaults.getFollowUpQuestionsCount(),
                defaults.getFollowUpQuestionsTemplate()
        );
    }

    @AfterEach
    void tearDown() {
        if (mockedChatModelFactory != null) {
            mockedChatModelFactory.close();
            mockedChatModelFactory = null;
        }
        AiService aiService = Injector.instantiateModelOrService(AiService.class);
        if (aiService != null) {
            try {
                aiService.close();
            } catch (Exception _) {
                // Ignore closing exceptions
            }
        }
        if (stage != null) {
            interact(stage::close);
        }
    }

    protected LibraryTab createNewLibrary() {
        interact(() -> new NewDatabaseAction(mainFrame, preferences).execute());
        LibraryTab libraryTab = mainFrame.getCurrentLibraryTab();
        assertNotNull(libraryTab);
        return libraryTab;
    }

    protected void insertAndEditEntry(LibraryTab libraryTab, BibEntry entry) {
        interact(() -> {
            StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);
            stateManager.setActiveDatabase(libraryTab.getBibDatabaseContext());
            if (!libraryTab.getDatabase().getEntries().contains(entry)) {
                libraryTab.getDatabase().insertEntry(entry);
            }
            libraryTab.showAndEdit(entry);
            stateManager.setSelectedEntries(List.of(entry));
        });
    }

    protected void editEntry(LibraryTab libraryTab, BibEntry entry) {
        interact(() -> {
            StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);
            stateManager.setActiveDatabase(libraryTab.getBibDatabaseContext());
            libraryTab.showAndEdit(entry);
            stateManager.setSelectedEntries(List.of(entry));
        });
    }

    protected Path createDummyPdfFile(String fileName) throws IOException {
        Path pdfPath = tempDir.resolve(fileName);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.showText("Sample PDF content for AI testing");
                contentStream.endText();
            }
            document.save(pdfPath.toFile());
        }
        return pdfPath;
    }

    protected void selectAiChatTab(BibEntry entry) {
        TabPane tabPane = lookup("#tabbed").queryAs(TabPane.class);
        assertNotNull(tabPane);

        Tab aiChatTab = tabPane.getTabs().stream()
                               .filter(AiChatTab.class::isInstance)
                               .findFirst()
                               .orElseThrow(() -> new AssertionError("AiChatTab not found in entry editor"));

        interact(() -> {
            tabPane.getSelectionModel().select(aiChatTab);
            ((AiChatTab) aiChatTab).notifyAboutFocus(entry);
        });
    }

    protected void selectAiChatTab() {
        TabPane tabPane = lookup("#tabbed").queryAs(TabPane.class);
        assertNotNull(tabPane);

        Tab aiChatTab = tabPane.getTabs().stream()
                               .filter(AiChatTab.class::isInstance)
                               .findFirst()
                               .orElseThrow(() -> new AssertionError("AiChatTab not found in entry editor"));

        interact(() -> tabPane.getSelectionModel().select(aiChatTab));
    }

    protected void waitForCondition(Supplier<Boolean> condition, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        AtomicBoolean result = new AtomicBoolean();
        while (System.currentTimeMillis() < deadline) {
            try {
                interact(() -> result.set(Boolean.TRUE.equals(condition.get())));
                if (result.get()) {
                    return;
                }
            } catch (Exception _) {
                // Ignore transient lookup exceptions during asynchronous UI updates
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        interact(() -> result.set(Boolean.TRUE.equals(condition.get())));
        if (!result.get()) {
            throw new AssertionError("Condition not met within " + timeout);
        }
    }
}
