package org.jabref.gui.ai.chat;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.ai.statuspane.SimpleStatusPaneView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ListScrollPane;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.AnswerEngineKind;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiChatView extends StackPane {
    private AiChatViewModel viewModel;

    @FXML private AiPrivacyNoticeView privacyNotice;
    @FXML private SimpleStatusPaneView noEntriesErrorPane;
    @FXML private VBox mainContainer;

    @FXML private ProgressIndicator loadingIndicator;
    @FXML private ListScrollPane<ChatHistoryRecordV2> chatHistoryScrollPane;

    @FXML private Button infoButton;
    @FXML private TextField userMessageTextField;
    @FXML private Button sendButton;
    @FXML private Button retryButton;
    @FXML private Button cancelButton;

    @FXML private Button clearButton;
    @FXML private Label aiModelLabel;

    @FXML private ComboBox<AnswerEngineKind> answerEngineCombo;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;
    @Inject private DialogService dialogService;

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
                dialogService
        );

        privacyNotice.managedProperty().bind(privacyNotice.visibleProperty());
        noEntriesErrorPane.managedProperty().bind(noEntriesErrorPane.visibleProperty());
        mainContainer.managedProperty().bind(mainContainer.visibleProperty());
        loadingIndicator.managedProperty().bind(loadingIndicator.visibleProperty());
        infoButton.managedProperty().bind(infoButton.visibleProperty());
        userMessageTextField.managedProperty().bind(userMessageTextField.visibleProperty());
        sendButton.managedProperty().bind(sendButton.visibleProperty());
        retryButton.managedProperty().bind(retryButton.visibleProperty());
        cancelButton.managedProperty().bind(cancelButton.visibleProperty());
        clearButton.managedProperty().bind(clearButton.visibleProperty());

        chatHistoryScrollPane.setItems(viewModel.getChatHistory());
        chatHistoryScrollPane.setRenderer(chatHistoryRecordV2 -> {
            AiChatMessageView aiChatMessageView = new AiChatMessageView();
            aiChatMessageView.setChatMessage(chatHistoryRecordV2);
            return aiChatMessageView;
        });

        answerEngineCombo.itemsProperty().bind(viewModel.answerEngineKindsProperty());
        answerEngineCombo.valueProperty().bindBidirectional(viewModel.selectedAnswerEngineKindProperty());
        new ViewModelListCellFactory<AnswerEngineKind>()
                .withText(AnswerEngineKind::getDisplayName)
                .install(answerEngineCombo);

        viewModel.stateProperty().addListener(_ -> updateByState());
        updateByState();

        viewModel.chatModelProperty().addListener(_ -> updateChatLabel());
        updateChatLabel();
    }

    public void setEntries(List<FullBibEntryAiIdentifier> entries) {
        viewModel.setEntries(entries);
    }

    public void setChatHistory(ObservableList<ChatHistoryRecordV2> chatHistory) {
        viewModel.setChatHistory(chatHistory);
    }

    private void updateByState() {
        switch (viewModel.stateProperty().get()) {
            case AI_TURNED_OFF -> {
                privacyNotice.setVisible(true);
                noEntriesErrorPane.setVisible(false);
                mainContainer.setVisible(false);
            }

            case NO_ENTRIES -> {
                privacyNotice.setVisible(false);
                noEntriesErrorPane.setVisible(true);
                mainContainer.setVisible(false);
            }

            case WAITING_FOR_MESSAGE -> {
                privacyNotice.setVisible(false);
                noEntriesErrorPane.setVisible(false);
                mainContainer.setVisible(true);

                loadingIndicator.setVisible(true);

                infoButton.setVisible(true);
                userMessageTextField.setVisible(true);
                userMessageTextField.setDisable(false);
                sendButton.setVisible(true);

                retryButton.setVisible(false);
                cancelButton.setVisible(true);

                clearButton.setDisable(false);
                answerEngineCombo.setDisable(false);
            }

            case ERROR -> {
                privacyNotice.setVisible(false);
                noEntriesErrorPane.setVisible(false);
                mainContainer.setVisible(true);

                loadingIndicator.setVisible(false);

                userMessageTextField.setVisible(false);

                sendButton.setVisible(false);

                retryButton.setVisible(true);
                cancelButton.setVisible(true);

                clearButton.setDisable(false);
                answerEngineCombo.setDisable(false);
            }

            case IDLE -> {
                privacyNotice.setVisible(false);
                noEntriesErrorPane.setVisible(false);
                mainContainer.setVisible(true);

                loadingIndicator.setVisible(false);

                userMessageTextField.setVisible(true);
                userMessageTextField.setEditable(true);
                userMessageTextField.setDisable(false);

                sendButton.setVisible(true);
                sendButton.setDisable(false);

                retryButton.setVisible(false);
                cancelButton.setVisible(false);

                clearButton.setDisable(false);
                answerEngineCombo.setDisable(false);
            }
        }
    }

    private void updateChatLabel() {
        ChatModel chatModel = viewModel.chatModelProperty().get();
        aiModelLabel.setText(Localization.lang(
                "Current chat model: %0 (%1)",
                chatModel.getName(),
                chatModel.getAiProvider().getDisplayName()
        ));
    }

    @FXML
    private void showInfo() {
        viewModel.showIngestionStatus();
    }

    @FXML
    private void send() {
        viewModel.sendMessage(userMessageTextField.getText());
    }

    @FXML
    private void retry() {
        viewModel.regenerate();
    }

    @FXML
    private void clearChatHistory() {
        viewModel.clearChatHistory();
    }
}
