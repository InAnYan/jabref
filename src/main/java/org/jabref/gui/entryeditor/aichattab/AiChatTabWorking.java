package org.jabref.gui.entryeditor.aichattab;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.AiChatComponent;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chat.AiChatLogic;
import org.jabref.logic.ai.chathistory.BibDatabaseChatHistory;
import org.jabref.logic.ai.chathistory.BibEntryChatHistory;
import org.jabref.logic.ai.chathistory.ChatMessage;
import org.jabref.logic.ai.embeddings.EmbeddingsGenerationTask;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatTabWorking {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatTabWorking.class);

    private final AiChatLogic aiChatLogic;
    private AiChatComponent aiChatComponent;
    private final Optional<BibEntryChatHistory> bibEntryChatHistory;

    public AiChatTabWorking(AiService aiService,
                            BibEntry entry,
                            BibDatabaseContext bibDatabaseContext,
                            EmbeddingsGenerationTask embeddingsGenerationTask,
                            TaskExecutor taskExecutor,
                            DialogService dialogService) {
        this.aiChatLogic = AiChatLogic.forBibEntry(aiService, entry);

        Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();

        if (databasePath.isEmpty() || entry.getCitationKey().isEmpty()) {
            this.bibEntryChatHistory = Optional.empty();
        } else {
            BibDatabaseChatHistory bibDatabaseChatHistory = aiService.getChatHistoryManager().getChatHistoryForBibDatabase(databasePath.get());
            this.bibEntryChatHistory = Optional.of(bibDatabaseChatHistory.getChatHistoryForEntry(entry.getCitationKey().get()));
        }

        aiChatComponent = new AiChatComponent(userPrompt -> {
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            LOGGER.trace("Threads at beginning of sending: {}", threadSet);
            threadSet.stream().filter(thread -> thread.getName().contains("pool-4-thread-1")).forEach(thread -> LOGGER.error("FOUND IT: {}", thread));

            ChatMessage userMessage = ChatMessage.user(userPrompt);
            // The aiChatComponent should be non-final to suppress an error in the next statement.
            aiChatComponent.addMessage(userMessage);
            addMessageToChatHistory(userMessage);

            aiChatComponent.setLoading(true);

            BackgroundTask.wrap(() -> {
                              LOGGER.debug("Sending a message to AI: {}", userPrompt);

                              Set<Thread> threadSet2 = Thread.getAllStackTraces().keySet();
                              LOGGER.trace("Threads before sending: {}", threadSet2);
                              threadSet2.stream().filter(thread -> thread.getName().contains("pool-4-thread-1")).forEach(thread -> LOGGER.error("FOUND IT: {}", thread));

                              // FIXME: This creates pool-4-thread-1, which causes trouble
                              String result = aiChatLogic.execute(userPrompt);

                              threadSet2 = Thread.getAllStackTraces().keySet();
                              LOGGER.trace("Threads after sending: {}", threadSet2);
                              threadSet2.stream().filter(thread -> thread.getName().contains("pool-4-thread-1")).forEach(thread -> LOGGER.error("FOUND IT: {}", thread));

                              return result;
                          })
                          .onSuccess(aiMessageText -> {
                              LOGGER.debug("Success");
                              aiChatComponent.setLoading(false);

                              ChatMessage aiMessage = ChatMessage.assistant(aiMessageText);
                              aiChatComponent.addMessage(aiMessage);
                              addMessageToChatHistory(aiMessage);

                              aiChatComponent.requestUserPromptTextFieldFocus();
                          })
                          .onFailure(e -> {
                              LOGGER.error("Got an error while sending a message to AI", e);
                              aiChatComponent.setLoading(false);
                              aiChatComponent.addError(e.getMessage());
                          })
                          .executeWith(taskExecutor);
        }, this::clearMessagesFromChatHistory, dialogService);

        restoreUIChatHistory();

        embeddingsGenerationTask.moveToFront(entry.getFiles());
        restoreLogicalChatHistory();
    }

    public Node getNode() {
        return aiChatComponent;
    }

    private void restoreLogicalChatHistory() {
        bibEntryChatHistory.ifPresent(entryChatHistory ->
                aiChatLogic.restoreMessages(entryChatHistory.getAllMessages()));
    }

    private void clearMessagesFromChatHistory() {
        bibEntryChatHistory.ifPresent(BibEntryChatHistory::clearMessages);
    }

    private void restoreUIChatHistory() {
        bibEntryChatHistory.ifPresent(entryChatHistory ->
                entryChatHistory.getAllMessages().forEach(aiChatComponent::addMessage));
    }

    private void addMessageToChatHistory(ChatMessage userMessage) {
        bibEntryChatHistory.ifPresent(entryChatHistory ->
                entryChatHistory.addMessage(userMessage));
    }
}
