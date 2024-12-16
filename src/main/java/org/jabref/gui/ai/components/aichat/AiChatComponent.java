package org.jabref.gui.ai.components.aichat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.chathistory.ChatHistoryComponent;
import org.jabref.gui.ai.components.aichat.chatprompt.ChatPromptComponent;
import org.jabref.gui.ai.components.util.Loadable;
import org.jabref.gui.ai.components.util.ProcessingState;
import org.jabref.gui.ai.components.util.ProcessingStatus;
import org.jabref.gui.ai.components.util.StatusesTableComponent;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.AiChatLogic;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.ai.util.ErrorMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.ListUtil;

import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatComponent extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatComponent.class);

    private final AiService aiService;
    private final ObservableList<BibEntry> entries;
    private final BibDatabaseContext bibDatabaseContext;
    private final AiPreferences aiPreferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    private final AiChatLogic aiChatLogic;

    private final ObservableList<ProcessingStatus> statusList = FXCollections.observableArrayList();
    private final StatusesTableComponent statusesTableComponent = new StatusesTableComponent(statusList);

    @FXML private Loadable uiLoadableChatHistory;
    @FXML private ChatHistoryComponent uiChatHistory;
    @FXML private Button statusesButton;
    @FXML private ChatPromptComponent chatPrompt;
    @FXML private Label noticeText;

    public AiChatComponent(AiService aiService,
                           StringProperty name,
                           ObservableList<ChatMessage> chatHistory,
                           ObservableList<BibEntry> entries,
                           BibDatabaseContext bibDatabaseContext,
                           AiPreferences aiPreferences,
                           FilePreferences filePreferences,
                           DialogService dialogService,
                           TaskExecutor taskExecutor
    ) {
        this.aiService = aiService;
        this.entries = entries;
        this.bibDatabaseContext = bibDatabaseContext;
        this.aiPreferences = aiPreferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.aiChatLogic = aiService.getAiChatService().makeChat(name, chatHistory, entries, bibDatabaseContext);

        ListUtil.getLinkedFiles(entries).forEach(linkedFile ->
                aiService.generateEmbeddingsTask(
                        linkedFile,
                        bibDatabaseContext,
                        filePreferences,
                        taskExecutor
                )
                        .onRunning(() -> updateStatus(linkedFile, ProcessingState.PROCESSING, ""))
                        .onFailure(ex -> updateStatus(linkedFile, ProcessingState.ERROR, ex.getMessage()))
                        .onSuccess((_) -> updateStatus(linkedFile, ProcessingState.SUCCESS, ""))
        );

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    private void updateStatus(LinkedFile linkedFile, ProcessingState state, String message) {
        Optional<ProcessingStatus> status = statusList.stream().filter(s -> s.getLinkedFile() == linkedFile).findFirst();

        if (status.isPresent()) {
            status.get().setProcessingState(state);
            status.get().setMessage(message);
        } else {
            statusList.add(new ProcessingStatus(linkedFile, state, message));
        }
    }

    @FXML
    public void initialize() {
        uiChatHistory.setItems(aiChatLogic.getChatHistory());
        initializeChatPrompt();
        initializeNotice();
    }

    private void initializeNotice() {
        String newNotice = noticeText
                .getText()
                .replaceAll("%0", aiPreferences.getAiProvider().getLabel() + " " + aiPreferences.getSelectedChatModel());

        noticeText.setText(newNotice);
    }

    private void initializeChatPrompt() {
        statusesButton.setOnAction(event -> {
            PopOver popOver = new PopOver(statusesTableComponent.getTable());
            popOver.show(statusesButton);
        });

        chatPrompt.setSendCallback(this::onSendMessage);

        chatPrompt.setCancelCallback(() -> chatPrompt.switchToNormalState());

        chatPrompt.setRetryCallback(userMessage -> {
            deleteLastMessage();
            deleteLastMessage();
            chatPrompt.switchToNormalState();
            onSendMessage(userMessage);
        });

        chatPrompt.requestPromptFocus();

        updatePromptHistory();
    }

    private void onSendMessage(String userPrompt) {
        UserMessage userMessage = new UserMessage(userPrompt);
        updatePromptHistory();
        setLoading(true);

        BackgroundTask<AiMessage> task =
                BackgroundTask
                        .wrap(() -> aiChatLogic.execute(userMessage))
                        .showToUser(true)
                        .onSuccess(aiMessage -> {
                            setLoading(false);
                            chatPrompt.requestPromptFocus();
                        })
                        .onFailure(e -> {
                            LOGGER.error("Got an error while sending a message to AI", e);
                            setLoading(false);

                            // Typically, if user has entered an invalid API base URL, we get either "401 - null" or "404 - null" strings.
                            // Since there might be other strings returned from other API endpoints, we use startsWith() here.
                            if (e.getMessage().startsWith("404") || e.getMessage().startsWith("401")) {
                                addError(Localization.lang("API base URL setting appears to be incorrect. Please check it in AI expert settings."));
                            } else {
                                addError(e.getMessage());
                            }

                            chatPrompt.switchToErrorState(userPrompt);
                        });

        task.titleProperty().set(Localization.lang("Waiting for AI reply..."));

        task.executeWith(taskExecutor);
    }

    private void addError(String error) {
        ErrorMessage chatMessage = new ErrorMessage(error);
        aiChatLogic.getChatHistory().add(chatMessage);
    }

    private void updatePromptHistory() {
        chatPrompt.getHistory().clear();
        chatPrompt.getHistory().addAll(getReversedUserMessagesStream().map(UserMessage::singleText).toList());
    }

    private Stream<UserMessage> getReversedUserMessagesStream() {
        return aiChatLogic
                .getChatHistory()
                .reversed()
                .stream()
                .filter(message -> message instanceof UserMessage)
                .map(UserMessage.class::cast);
    }

    private void setLoading(boolean loading) {
        uiLoadableChatHistory.setLoading(loading);
        chatPrompt.setDisableToButtons(loading);
    }

    @FXML
    private void onClearChatHistory() {
        boolean agreed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Clear chat history"),
                Localization.lang("Are you sure you want to clear the chat history of this entry?")
        );

        if (agreed) {
            aiChatLogic.getChatHistory().clear();
        }
    }

    private void deleteLastMessage() {
        if (!aiChatLogic.getChatHistory().isEmpty()) {
            int index = aiChatLogic.getChatHistory().size() - 1;
            aiChatLogic.getChatHistory().remove(index);
        }
    }
}
