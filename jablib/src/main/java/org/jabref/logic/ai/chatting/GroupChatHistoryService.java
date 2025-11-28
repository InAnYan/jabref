package org.jabref.logic.ai.chatting;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.GroupChatHistoryRepository;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.GroupTreeNode;

import dev.langchain4j.data.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupChatHistoryService implements AutoCloseable {
    private final Logger LOGGER = LoggerFactory.getLogger(GroupChatHistoryService.class);

    private final GroupChatHistoryRepository groupChatHistoryRepository;

    // We use {@link TreeMap} for group chat history for the same reason as for {@link BibEntry}ies.
    private final TreeMap<GroupTreeNode, ChatHistoryManagementRecord> groupsChatHistory =
            new TreeMap<>(Comparator.comparing(GroupTreeNode::getName));

    public GroupChatHistoryService(
            GroupChatHistoryRepository groupChatHistoryRepository
    ) {
        this.groupChatHistoryRepository = groupChatHistoryRepository;
    }

    public void setupDatabase(BibDatabaseContext bibDatabaseContext) {
        bibDatabaseContext.getMetaData().getGroups().ifPresent(rootGroupTreeNode ->
                rootGroupTreeNode.iterateOverTree().forEach(groupNode -> {
                    groupNode.getGroup().nameProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue != null && oldValue != null) {
                            transferHistory(bibDatabaseContext, groupNode, oldValue, newValue);
                        }
                    });

                    groupNode.getGroupProperty().addListener((obs, oldValue, newValue) -> {
                        if (oldValue != null && newValue != null) {
                            transferHistory(bibDatabaseContext, groupNode, oldValue.getName(), newValue.getName());
                        }
                    });
                }));
    }

    private void transferHistory(BibDatabaseContext bibDatabaseContext, GroupTreeNode groupTreeNode, String oldName, String newName) {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.warn("Could not transfer chat history of group {} (old name: {}): database path is empty.", newName, oldName);
            return;
        }

        List<ChatMessage> chatMessages = groupsChatHistory.computeIfAbsent(groupTreeNode,
                e -> new ChatHistoryManagementRecord(Optional.of(bibDatabaseContext), FXCollections.observableArrayList())).chatHistory();

        GroupAiIdentifier oldIdentifier = new GroupAiIdentifier(bibDatabaseContext.getDatabasePath().get(), oldName);
        GroupAiIdentifier newIdentifier = new GroupAiIdentifier(bibDatabaseContext.getDatabasePath().get(), newName);

        groupChatHistoryRepository.storeMessagesForGroup(oldIdentifier, List.of());
        groupChatHistoryRepository.storeMessagesForGroup(newIdentifier, chatMessages);
    }

    public ObservableList<ChatMessage> getChatHistory(BibDatabaseContext bibDatabaseContext, GroupTreeNode group) {
        return groupsChatHistory.computeIfAbsent(group, groupArg -> {
            ObservableList<ChatMessage> chatHistory;

            if (bibDatabaseContext.getDatabasePath().isEmpty()) {
                chatHistory = FXCollections.observableArrayList();
            } else {
                GroupAiIdentifier identifier = new GroupAiIdentifier(bibDatabaseContext.getDatabasePath().get(), group.getGroup().getName());
                List<ChatMessage> chatMessagesList = groupChatHistoryRepository.loadMessagesForGroup(
                        identifier
                );

                chatHistory = FXCollections.observableArrayList(chatMessagesList);
            }

            return new ChatHistoryManagementRecord(Optional.of(bibDatabaseContext), chatHistory);
        }).chatHistory();
    }

    /**
     * Removes the chat history for the given {@link GroupTreeNode} from the internal RAM map.
     * If the {@link GroupTreeNode} satisfies requirements for serialization and deserialization of chat history (see
     * the docstring for the {@link EntryChatHistoryService}), then the chat history will be stored via the
     * {@link EntryChatHistoryRepository}.
     * <p>
     * It is not necessary to call this method (everything will be stored in {@link EntryChatHistoryService#close()},
     * but it's best to call it when the chat history {@link GroupTreeNode} is no longer needed.
     */
    public void closeChatHistory(GroupTreeNode group) {
        ChatHistoryManagementRecord chatHistoryManagementRecord = groupsChatHistory.get(group);
        if (chatHistoryManagementRecord == null) {
            return;
        }

        Optional<BibDatabaseContext> bibDatabaseContext = chatHistoryManagementRecord.bibDatabaseContext();

        if (bibDatabaseContext.isPresent() && bibDatabaseContext.get().getDatabasePath().isPresent()) {
            GroupAiIdentifier identifier = new GroupAiIdentifier(bibDatabaseContext.get().getDatabasePath().get(), group.getGroup().getName());
            groupChatHistoryRepository.storeMessagesForGroup(
                    identifier,
                    chatHistoryManagementRecord.chatHistory()
            );
        }

        // TODO: What if there is two AI chats for the same entry? And one is closed and one is not?
        groupsChatHistory.remove(group);
    }


    @Override
    public void close() throws Exception {
        // Clone is for the same reason, as written above.
        new HashSet<>(groupsChatHistory.keySet()).forEach(this::closeChatHistory);
    }
}
