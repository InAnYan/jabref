package org.jabref.logic.ai.chatting;

import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.chatting.listeners.EntryChattingDatabaseListener;
import org.jabref.logic.ai.chatting.listeners.GroupChattingDatabaseListener;
import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.GroupChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.MVStoreEntryChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.MVStoreGroupChatHistoryRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.database.BibDatabaseContext;

public class ChattingAiFeature implements AiFeature {
    private static final String ENTRY_CHAT_HISTORY_FILE_NAME = "entries-chat-histories.mv"; // v2
    private static final String GROUP_CHAT_HISTORY_FILE_NAME = "groups-chat-histories.mv"; // v2

    private final MVStoreEntryChatHistoryRepository mvStoreEntryChatHistoryStorage;
    private final MVStoreGroupChatHistoryRepository mvStoreGroupChatHistoryStorage;

    private final CurrentChatLanguageModel currentChatLanguageModel;

    private final EntryChattingDatabaseListener entryChattingDatabaseListener;
    private final GroupChattingDatabaseListener groupChattingDatabaseListener;

    public ChattingAiFeature(
            AiPreferences aiPreferences,
            NotificationService notificationService
    ) {
        this.mvStoreEntryChatHistoryStorage = new MVStoreEntryChatHistoryRepository(
                Directories.getAiFilesDirectory().resolve(ENTRY_CHAT_HISTORY_FILE_NAME),
                notificationService
        );
        this.mvStoreGroupChatHistoryStorage = new MVStoreGroupChatHistoryRepository(
                Directories.getAiFilesDirectory().resolve(GROUP_CHAT_HISTORY_FILE_NAME),
                notificationService
        );

        this.currentChatLanguageModel = new CurrentChatLanguageModel(
                aiPreferences,
                new CurrentTokenEstimator(aiPreferences)
        );

        this.entryChattingDatabaseListener = new EntryChattingDatabaseListener(
                mvStoreEntryChatHistoryStorage
        );
        this.groupChattingDatabaseListener = new GroupChattingDatabaseListener(
                mvStoreGroupChatHistoryStorage
        );
    }

    public void setupDatabase(BibDatabaseContext databaseContext) {
        entryChattingDatabaseListener.setupDatabase(databaseContext);
        groupChattingDatabaseListener.setupDatabase(databaseContext);
    }

    public CurrentChatLanguageModel getCurrentChatModel() {
        return currentChatLanguageModel;
    }

    public EntryChatHistoryRepository getEntryChatHistoryRepository() {
        return mvStoreEntryChatHistoryStorage;
    }

    public GroupChatHistoryRepository getGroupChatHistoryRepository() {
        return mvStoreGroupChatHistoryStorage;
    }

    @Override
    public void close() throws Exception {
        mvStoreEntryChatHistoryStorage.close();
        mvStoreGroupChatHistoryStorage.close();
    }
}
