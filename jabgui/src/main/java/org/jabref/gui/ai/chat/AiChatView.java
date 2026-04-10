package org.jabref.gui.ai.chat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.ai.statuspane.SimpleStatusPaneView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.HistoryTextArea;
import org.jabref.gui.util.ListScrollPane;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.AiChatJsonExporter;
import org.jabref.logic.ai.chatting.AiChatMarkdownExporter;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatView extends StackPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatView.class);

    private final AiChatStatusWindow aiChatStatusWindow = new AiChatStatusWindow();

    @FXML private AiPrivacyNoticeView privacyNotice;
    @FXML private SimpleStatusPaneView noFilesErrorPane;
    @FXML private BorderPane mainContainer;

    @FXML private ProgressIndicator loadingIndicator;
    @FXML private ListScrollPane<ChatMessage> chatHistoryScrollPane;

    @FXML private Button infoButton;
    @FXML private HistoryTextArea userMessageTextArea;
    @FXML private Button sendButton;
    @FXML private Button retryButton;
    @FXML private Button cancelButton;

    @FXML private Button clearButton;
    @FXML private Label aiModelLabel;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private BibEntryTypesManager entryTypesManager;

    private AiChatViewModel viewModel;

    public AiChatView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    private static String formatChatModelLabel(ChatModel chatModel) {
        if (chatModel == null) {
            return "";
        }

        return Localization.lang(
                "Current chat model: %0 %1",
                chatModel.getAiProvider().getDisplayName(),
                chatModel.getName()
        );
    }

    @FXML
    private void initialize() {
        viewModel = new AiChatViewModel(
                preferences.getAiPreferences(),
                preferences.getFilePreferences(),
                preferences.getAiPreferences().getChattingSystemMessageTemplate(),
                preferences.getAiPreferences().getChattingUserMessageTemplate(),
                aiService.getIngestionTaskAggregator(),
                aiService.getIngestedDocumentsRepository(),
                dialogService,
                aiService.getEmbeddingsStore(),
                taskExecutor
        );

        setupBindings();
        setupValues();
    }

    private void setupBindings() {
        viewModel.answerEngineProperty().bind(aiChatStatusWindow.answerEngineProperty());
        aiChatStatusWindow.entriesProperty().bind(viewModel.entriesProperty());
        aiChatStatusWindow.generateEmbeddingsTasksProperty().bind(viewModel.generateEmbeddingsTasksProperty());

        chatHistoryScrollPane.itemsProperty().bind(viewModel.chatHistoryProperty());
        chatHistoryScrollPane.setRenderer(this::renderChatMessage);

        privacyNotice.managedProperty().bind(privacyNotice.visibleProperty());
        noFilesErrorPane.managedProperty().bind(noFilesErrorPane.visibleProperty());
        mainContainer.managedProperty().bind(mainContainer.visibleProperty());
        loadingIndicator.managedProperty().bind(loadingIndicator.visibleProperty());
        infoButton.managedProperty().bind(infoButton.visibleProperty());
        userMessageTextArea.managedProperty().bind(userMessageTextArea.visibleProperty());
        sendButton.managedProperty().bind(sendButton.visibleProperty());
        retryButton.managedProperty().bind(retryButton.visibleProperty());
        cancelButton.managedProperty().bind(cancelButton.visibleProperty());
        clearButton.managedProperty().bind(clearButton.visibleProperty());

        BooleanBinding isAiTurnedOff = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.AI_TURNED_OFF);
        BooleanBinding isNoFiles = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.NO_FILES);
        BooleanBinding isWaiting = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.WAITING_FOR_MESSAGE);
        BooleanBinding isError = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.ERROR);
        BooleanBinding isIdle = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.IDLE);

        privacyNotice.visibleProperty().bind(isAiTurnedOff);
        noFilesErrorPane.visibleProperty().bind(isNoFiles);
        mainContainer.visibleProperty().bind(isAiTurnedOff.not().and(isNoFiles.not()));

        loadingIndicator.visibleProperty().bind(isWaiting);
        userMessageTextArea.visibleProperty().bind(isIdle);
        sendButton.visibleProperty().bind(isIdle);
        retryButton.visibleProperty().bind(isError);
        cancelButton.visibleProperty().bind(isWaiting.or(isError));

        aiModelLabel.textProperty().bind(viewModel.chatModelProperty().map(AiChatView::formatChatModelLabel));
    }

    private void setupValues() {
        userMessageTextArea.getHistory().addAll(
                viewModel
                        .chatHistoryProperty()
                        .stream()
                        .map(ChatMessage::content)
                        .filter(StringUtil::isNotBlank)
                        .toList()
        );
    }

    private Node renderChatMessage(ChatMessage chatMessage) {
        AiChatMessageView aiChatMessageView = new AiChatMessageView();

        aiChatMessageView.setChatMessage(chatMessage);
        aiChatMessageView.setOnDelete(_ -> viewModel.delete(chatMessage.id()));
        aiChatMessageView.setOnRegenerate(_ -> viewModel.regenerate(chatMessage.id()));

        return aiChatMessageView;
    }

    @FXML
    private void showInfo() {
        dialogService.showCustomDialogAndWait(aiChatStatusWindow);
    }

    @FXML
    private void send() {
        viewModel.sendMessage(userMessageTextArea.getText());
        userMessageTextArea.clear();
    }

    @FXML
    private void retry() {
        viewModel.regenerate();
    }

    @FXML
    private void cancel() {
        viewModel.cancel();
    }

    @FXML
    private void clearChatHistory() {
        viewModel.chatHistoryProperty().get().clear();
    }

    @FXML
    private void exportMarkdown() {
        List<ChatMessage> messages = viewModel.chatHistoryProperty().get();

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
                             AiChatMarkdownExporter exporter = new AiChatMarkdownExporter(entryTypesManager, preferences.getFieldPreferences());
                             String content = exporter.export(getExportEntries(), getExportDatabaseMode(), messages);
                             Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             dialogService.notify(Localization.lang("Export operation finished successfully."));
                         } catch (IOException e) {
                             LOGGER.error("Problem occurred while writing the export file", e);
                             dialogService.showErrorDialogAndWait(Localization.lang("Problem occurred while writing the export file"), e);
                         }
                     });
    }

    @FXML
    private void exportJson() {
        List<ChatMessage> messages = viewModel.chatHistoryProperty().get();

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
                             ChatModel chatModel = viewModel.chatModelProperty().get();
                             String provider = chatModel != null ? chatModel.getAiProvider().getDisplayName() : "";
                             String model = chatModel != null ? chatModel.getName() : "";

                             AiChatJsonExporter exporter = new AiChatJsonExporter(entryTypesManager, preferences.getFieldPreferences());
                             String content = exporter.export(provider, model, getExportEntries(), getExportDatabaseMode(), messages);
                             Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             dialogService.notify(Localization.lang("Export operation finished successfully."));
                         } catch (IOException e) {
                             LOGGER.error("Problem occurred while writing the export file", e);
                             dialogService.showErrorDialogAndWait(Localization.lang("Problem occurred while writing the export file"), e);
                         }
                     });
    }

    private List<BibEntry> getExportEntries() {
        return viewModel.entriesProperty().stream()
                        .map(FullBibEntry::entry)
                        .toList();
    }

    private BibDatabaseMode getExportDatabaseMode() {
        return viewModel.entriesProperty().isEmpty()
                ? BibDatabaseMode.BIBTEX
                : viewModel.entriesProperty().getFirst().databaseContext().getMode();
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return viewModel.chatHistoryProperty();
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return viewModel.entriesProperty();
    }
}
