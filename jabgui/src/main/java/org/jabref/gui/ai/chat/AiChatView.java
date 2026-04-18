package org.jabref.gui.ai.chat;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.ai.statuspane.UniversalStatusPaneView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.HistoryTextArea;
import org.jabref.gui.util.ListScrollPane;
import org.jabref.gui.util.SimpleListView;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiChatView extends StackPane {
    private final AiChatStatusWindow aiChatStatusWindow = new AiChatStatusWindow();

    @FXML private AiPrivacyNoticeView privacyNotice;
    @FXML private UniversalStatusPaneView noFilesErrorPane;
    @FXML private BorderPane mainContainer;

    @FXML private ListScrollPane<ChatMessage> chatHistoryScrollPane;

    @FXML private Pane transparentPane;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private HBox followUpQuestionsArea;
    @FXML private SimpleListView<String> followUpQuestionsSimpleListView;

    @FXML private Button infoButton;
    @FXML private HistoryTextArea userMessageTextArea;
    @FXML private Button sendButton;
    @FXML private Button retryButton;
    @FXML private Button cancelButton;

    @FXML private Label noticeText;

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

    @FXML
    private void initialize() {
        viewModel = new AiChatViewModel(
                preferences.getAiPreferences(),
                preferences.getFilePreferences(),
                aiService.getIngestionTaskAggregator(),
                aiService.getIngestedDocumentsRepository(),
                dialogService,
                aiService.getEmbeddingsStore(),
                aiService.getEmbeddingModelCache(),
                taskExecutor
        );

        setupBindings();
        setupValues();
        setupFollowUpQuestions();
    }

    private void setupBindings() {
        viewModel.answerEngineProperty().bind(aiChatStatusWindow.answerEngineProperty());
        viewModel.chatModelProperty().bind(aiChatStatusWindow.chatModelProperty());

        aiChatStatusWindow.entriesProperty().bind(viewModel.entriesProperty());
        aiChatStatusWindow.generateEmbeddingsTasksProperty().bind(viewModel.generateEmbeddingsTasksProperty());
        aiChatStatusWindow.chatHistoryProperty().bind(viewModel.chatHistoryProperty());

        chatHistoryScrollPane.itemsProperty().bind(viewModel.chatHistoryProperty());
        chatHistoryScrollPane.setRenderer(this::renderChatMessage);
        chatHistoryScrollPane.setAutoScrollToBottom(true);

        privacyNotice.managedProperty().bind(privacyNotice.visibleProperty());
        noFilesErrorPane.managedProperty().bind(noFilesErrorPane.visibleProperty());
        mainContainer.managedProperty().bind(mainContainer.visibleProperty());
        loadingIndicator.managedProperty().bind(loadingIndicator.visibleProperty());
        transparentPane.managedProperty().bind(transparentPane.visibleProperty());
        infoButton.managedProperty().bind(infoButton.visibleProperty());
        userMessageTextArea.managedProperty().bind(userMessageTextArea.visibleProperty());
        sendButton.managedProperty().bind(sendButton.visibleProperty());
        retryButton.managedProperty().bind(retryButton.visibleProperty());
        cancelButton.managedProperty().bind(cancelButton.visibleProperty());
        followUpQuestionsArea.managedProperty().bind(followUpQuestionsArea.visibleProperty());

        BooleanBinding isAiTurnedOff = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.AI_TURNED_OFF);
        BooleanBinding isNoFiles = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.NO_FILES);
        BooleanBinding isWaiting = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.WAITING_FOR_MESSAGE);
        BooleanBinding isError = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.ERROR);
        BooleanBinding isIdle = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.IDLE);

        privacyNotice.visibleProperty().bind(isAiTurnedOff);
        noFilesErrorPane.visibleProperty().bind(isNoFiles);
        mainContainer.visibleProperty().bind(isAiTurnedOff.not().and(isNoFiles.not()));

        loadingIndicator.visibleProperty().bind(isWaiting);
        transparentPane.visibleProperty().bind(isWaiting);
        userMessageTextArea.visibleProperty().bind(isIdle);
        sendButton.visibleProperty().bind(isIdle);
        retryButton.visibleProperty().bind(isError);
        cancelButton.visibleProperty().bind(isWaiting.or(isError));
        noticeText.textProperty().bind(viewModel.chatModelProperty().map(this::formatNoticeText));
    }

    private String formatNoticeText(ChatModel model) {
        String modelName = model.getAiProvider().getDisplayName() + " " + model.getName();
        return Localization.lang("Current AI model: %0. The AI may generate inaccurate or inappropriate responses. Please verify any information provided", modelName);
    }

    private void setupFollowUpQuestions() {
        followUpQuestionsArea.visibleProperty().bind(
                viewModel.followUpQuestionsProperty().emptyProperty().not()
                         .and(preferences.getAiPreferences().generateFollowUpQuestionsProperty())
                         .and(viewModel.stateProperty().isEqualTo(AiChatViewModel.State.IDLE))
        );

        followUpQuestionsSimpleListView.itemsProperty().bind(viewModel.followUpQuestionsProperty());
        followUpQuestionsSimpleListView.setRenderer(question -> {
            Button button = new Button(question);
            button.getStyleClass().add("exampleQuestionStyle");
            button.setOnAction(_ -> viewModel.sendFollowUpMessage(question));
            return button;
        });
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

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return viewModel.chatHistoryProperty();
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return viewModel.entriesProperty();
    }
}
