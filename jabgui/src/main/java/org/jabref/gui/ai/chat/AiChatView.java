package org.jabref.gui.ai.chat;

import java.util.Optional;

import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
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
import org.jabref.logic.ai.chatting.util.ChatHistoryRecordUtils;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.util.ChatMessagesUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiChatView extends StackPane {
    private final AiChatStatusWindow aiChatStatusWindow = new AiChatStatusWindow();

    @FXML private AiPrivacyNoticeView privacyNotice;
    @FXML private SimpleStatusPaneView noFilesErrorPane;
    @FXML private BorderPane mainContainer;

    @FXML private ProgressIndicator loadingIndicator;
    @FXML private ListScrollPane<ChatHistoryRecordV2> chatHistoryScrollPane;

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

    @FXML
    private void initialize() {
        viewModel = new AiChatViewModel(
                preferences,
                aiService,
                taskExecutor
        );

        viewModel.answerEngineProperty().bind(aiChatStatusWindow.answerEngineProperty());
        aiChatStatusWindow.entriesProperty().bind(viewModel.entriesProperty());
        aiChatStatusWindow.generateEmbeddingsTasksProperty().bind(viewModel.generateEmbeddingsTasksProperty());

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

        userMessageTextArea.getHistory().addAll(
                viewModel
                        .chatHistoryProperty()
                        .stream()
                        .map(ChatHistoryRecordUtils::convertRecordToLangchain)
                        .map(ChatMessagesUtil::getContent)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList()
        );

        chatHistoryScrollPane.setRenderer(chatHistoryRecordV2 -> {
            AiChatMessageView aiChatMessageView = new AiChatMessageView();
            aiChatMessageView.setChatMessage(chatHistoryRecordV2);
            aiChatMessageView.setOnDelete(_ -> viewModel.delete(chatHistoryRecordV2.id()));
            aiChatMessageView.setOnRegenerate(_ -> viewModel.regenerate(chatHistoryRecordV2.id()));
            return aiChatMessageView;
        });
        chatHistoryScrollPane.itemsProperty().bind(viewModel.chatHistoryProperty());

        viewModel.stateProperty().addListener((_, _, value) -> updateByState(value));
        updateByState(viewModel.stateProperty().get());

        viewModel.chatModelProperty().addListener(_ -> updateChatLabel());
        updateChatLabel();
    }

    private void updateByState(AiChatViewModel.State state) {
        switch (state) {
            case AI_TURNED_OFF -> {
                privacyNotice.setVisible(true);
                noFilesErrorPane.setVisible(false);
                mainContainer.setVisible(false);
            }

            case NO_FILES -> {
                privacyNotice.setVisible(false);
                noFilesErrorPane.setVisible(true);
                mainContainer.setVisible(false);
            }

            case WAITING_FOR_MESSAGE -> {
                privacyNotice.setVisible(false);
                noFilesErrorPane.setVisible(false);
                mainContainer.setVisible(true);

                loadingIndicator.setVisible(true);

                infoButton.setVisible(true);
                infoButton.setDisable(false);
                userMessageTextArea.setVisible(false);
                userMessageTextArea.setDisable(false);

                sendButton.setVisible(false);
                retryButton.setVisible(false);
                cancelButton.setVisible(true);

                clearButton.setDisable(false);
            }

            case ERROR -> {
                privacyNotice.setVisible(false);
                noFilesErrorPane.setVisible(false);
                mainContainer.setVisible(true);

                loadingIndicator.setVisible(false);

                userMessageTextArea.setVisible(false);

                sendButton.setVisible(false);

                retryButton.setVisible(true);
                cancelButton.setVisible(true);

                clearButton.setDisable(false);
            }

            case IDLE -> {
                privacyNotice.setVisible(false);
                noFilesErrorPane.setVisible(false);
                mainContainer.setVisible(true);

                loadingIndicator.setVisible(false);

                userMessageTextArea.setVisible(true);
                userMessageTextArea.setEditable(true);
                userMessageTextArea.setDisable(false);

                sendButton.setVisible(true);
                sendButton.setDisable(false);

                retryButton.setVisible(false);
                cancelButton.setVisible(false);

                clearButton.setDisable(false);
            }
        }
    }

    private void updateChatLabel() {
        ChatModel chatModel = viewModel.chatModelProperty().get();
        aiModelLabel.setText(Localization.lang(
                "Current chat model: %0 %1",
                chatModel.getAiProvider().getDisplayName(),
                chatModel.getName()
        ));
    }

    @FXML
    private void privacyDisagree() {
        viewModel.privacyDisagree();
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

    public ListProperty<ChatHistoryRecordV2> chatHistoryProperty() {
        return viewModel.chatHistoryProperty();
    }

    public ListProperty<BibEntryAiIdentifier> entriesProperty() {
        return viewModel.entriesProperty();
    }
}
