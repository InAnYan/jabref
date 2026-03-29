package org.jabref.logic.ai.chatting;

import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.chatting.listeners.EntryChattingDatabaseListener;
import org.jabref.logic.ai.chatting.listeners.GroupChattingDatabaseListener;
import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.MVStoreChatHistoryRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;

public class ChattingAiFeature extends AiFeature {
    private static final String CHAT_HISTORY_FILE_NAME = "chat-histories.mv"; // v2

    private final MVStoreChatHistoryRepository mvStoreChatHistoryRepository;

    private final CurrentChatLanguageModel currentChatLanguageModel;

    public ChattingAiFeature(
            AiPreferences aiPreferences,
            NotificationService notificationService
    ) {
        this.mvStoreChatHistoryRepository = new MVStoreChatHistoryRepository(
                Directories.getAiFilesDirectory().resolve(CHAT_HISTORY_FILE_NAME),
                notificationService
        );

        this.currentChatLanguageModel = new CurrentChatLanguageModel(
                aiPreferences,
                new CurrentTokenEstimator(aiPreferences)
        );

        databaseListeners.add(new EntryChattingDatabaseListener(mvStoreChatHistoryRepository));
        databaseListeners.add(new GroupChattingDatabaseListener(mvStoreChatHistoryRepository));
    }

    public CurrentChatLanguageModel getCurrentChatModel() {
        return currentChatLanguageModel;
    }

    public ChatHistoryRepository getChatHistoryRepository() {
        return mvStoreChatHistoryRepository;
    }

    @Override
    public void close() throws Exception {
        mvStoreChatHistoryRepository.close();
    }
}
