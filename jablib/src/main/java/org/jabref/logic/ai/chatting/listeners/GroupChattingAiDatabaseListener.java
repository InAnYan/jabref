package org.jabref.logic.ai.chatting.listeners;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.AiDatabaseListener;
import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.chatting.ChatType;
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
        Optional<String> aiLibraryId = bibDatabaseContext.getMetaData().getAiLibraryId();

        if (aiLibraryId.isEmpty()) {
            LOGGER.warn("Could not transfer chat history of group {} (old key: {}): AI library ID is empty.", oldName, newName);
            return;
        }

        ChatIdentifier oldIdentifier = new ChatIdentifier(aiLibraryId.get(), ChatType.WITH_GROUP, oldName);
        ChatIdentifier newIdentifier = new ChatIdentifier(aiLibraryId.get(), ChatType.WITH_GROUP, newName);

        List<ChatMessage> chatHistory = chatHistoryRepository.getAllMessages(oldIdentifier);

        chatHistoryRepository.clear(oldIdentifier);
        chatHistoryRepository.clear(newIdentifier);

        chatHistory.forEach(record -> chatHistoryRepository.addMessage(newIdentifier, record));
    }

    @Override
    public void close() throws Exception {
        // Nothing to close.
    }
}
