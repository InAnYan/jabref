package org.jabref.gui.ai.chat;

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
import org.jabref.gui.util.HistoryTextArea;
import org.jabref.gui.util.ListScrollPane;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiChatView extends StackPane {
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
                aiService.getChattingFeature().getCurrentChatModel(),
                aiService.getTemplatesFeature().getCurrentAiTemplates(),
                aiService.getIngestionFeature().getIngestionTaskAggregator(),
                aiService.getIngestionFeature().getIngestedDocumentsRepository(),
                aiService.getEmbeddingFeature().getCurrentEmbeddingModel(),
                aiService.getIngestionFeature().getEmbeddingsStore(),
                aiService.getIngestionFeature().getCurrentDocumentSplitter(),
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

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return viewModel.chatHistoryProperty();
    }

    public ListProperty<BibEntryAiIdentifier> entriesProperty() {
        return viewModel.entriesProperty();
    }
}
