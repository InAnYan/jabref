package org.jabref.logic.ai.chatting.listeners;

import java.util.List;

import org.jabref.logic.ai.AiDatabaseListener;
import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.chatting.GroupChatHistoryIdentifier;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupChattingAiDatabaseListener implements AiDatabaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupChattingAiDatabaseListener.class);

    private final ChatHistoryRepository chatHistoryRepository;

    public GroupChattingAiDatabaseListener(
            ChatHistoryRepository chatHistoryRepository
    ) {
        this.chatHistoryRepository = chatHistoryRepository;
    }

    @Override
    public void setupDatabase(BibDatabaseContext databaseContext) {
        databaseContext.getMetaData().getGroups().ifPresent(rootGroupTreeNode ->
                rootGroupTreeNode.iterateOverTree().forEach(groupNode -> {
                    groupNode.getGroup().nameProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue != null && oldValue != null) {
                            transferHistory(databaseContext, oldValue, newValue);
                        }
                    });

                    groupNode.getGroupProperty().addListener((obs, oldValue, newValue) -> {
                        if (oldValue != null && newValue != null) {
                            transferHistory(databaseContext, oldValue.getName(), newValue.getName());
                        }
                    });
                }));
    }

    // TODO: Generalize transfer
    private void transferHistory(BibDatabaseContext bibDatabaseContext, String oldName, String newName) {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.warn("Could not transfer chat history of group {} (old name: {}): database path is empty.", newName, oldName);
            return;
        }

        GroupChatHistoryIdentifier oldIdentifier = new GroupChatHistoryIdentifier(bibDatabaseContext.getDatabasePath().get(), oldName);
        GroupChatHistoryIdentifier newIdentifier = new GroupChatHistoryIdentifier(bibDatabaseContext.getDatabasePath().get(), newName);

        List<ChatMessage> chatHistory = chatHistoryRepository.getAllMessages(oldIdentifier);

        chatHistoryRepository.clear(oldIdentifier);
        chatHistoryRepository.clear(newIdentifier);

        chatHistory.forEach(record -> chatHistoryRepository.addMessage(newIdentifier, record));
    }
}
