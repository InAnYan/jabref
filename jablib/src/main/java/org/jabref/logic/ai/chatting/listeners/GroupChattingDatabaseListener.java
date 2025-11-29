package org.jabref.logic.ai.chatting.listeners;

import java.util.List;

import org.jabref.logic.ai.chatting.repositories.GroupChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.GroupTreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupChattingDatabaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupChattingDatabaseListener.class);

    private final GroupChatHistoryRepository groupChatHistoryRepository;

    public GroupChattingDatabaseListener(GroupChatHistoryRepository groupChatHistoryRepository) {
        this.groupChatHistoryRepository = groupChatHistoryRepository;
    }

    public void setupDatabase(BibDatabaseContext databaseContext) {
        databaseContext.getMetaData().getGroups().ifPresent(rootGroupTreeNode ->
                rootGroupTreeNode.iterateOverTree().forEach(groupNode -> {
                    groupNode.getGroup().nameProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue != null && oldValue != null) {
                            transferHistory(databaseContext, groupNode, oldValue, newValue);
                        }
                    });

                    groupNode.getGroupProperty().addListener((obs, oldValue, newValue) -> {
                        if (oldValue != null && newValue != null) {
                            transferHistory(databaseContext, groupNode, oldValue.getName(), newValue.getName());
                        }
                    });
                }));
    }

    private void transferHistory(BibDatabaseContext bibDatabaseContext, GroupTreeNode groupTreeNode, String oldName, String newName) {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.warn("Could not transfer chat history of group {} (old name: {}): database path is empty.", newName, oldName);
            return;
        }

        GroupAiIdentifier oldIdentifier = new GroupAiIdentifier(bibDatabaseContext.getDatabasePath().get(), oldName);
        GroupAiIdentifier newIdentifier = new GroupAiIdentifier(bibDatabaseContext.getDatabasePath().get(), newName);

        List<ChatHistoryRecordV2> chatHistory = groupChatHistoryRepository.getAllMessages(oldIdentifier);

        groupChatHistoryRepository.clear(oldIdentifier);
        groupChatHistoryRepository.clear(newIdentifier);

        chatHistory.forEach(record -> groupChatHistoryRepository.addMessage(newIdentifier, record));
    }
}
