package org.jabref.logic.ai.chatting;

import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.chatting.listeners.EntryChattingAiDatabaseListener;
import org.jabref.logic.ai.chatting.listeners.GroupChattingAiDatabaseListener;
import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.MVStoreChatHistoryRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.database.BibDatabaseContext;

public class ChattingAiFeature implements AiFeature {
    private static final String CHAT_HISTORY_FILE_NAME = "chat-histories.mv"; // v2

    private final MVStoreChatHistoryRepository mvStoreChatHistoryRepository;

    private final CurrentChatLanguageModel currentChatLanguageModel;

    private final EntryChattingAiDatabaseListener entryChattingAiDatabaseListener;
    private final GroupChattingAiDatabaseListener groupChattingAiDatabaseListener;

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

        this.entryChattingAiDatabaseListener = new EntryChattingAiDatabaseListener(mvStoreChatHistoryRepository);
        this.groupChattingAiDatabaseListener = new GroupChattingAiDatabaseListener(mvStoreChatHistoryRepository);
    }

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        entryChattingAiDatabaseListener.setupDatabase(context);
        groupChattingAiDatabaseListener.setupDatabase(context);
    }

    public CurrentChatLanguageModel getCurrentChatModel() {
        return currentChatLanguageModel;
    }

    public ChatHistoryRepository getChatHistoryRepository() {
        return mvStoreChatHistoryRepository;
    }

    @Override
    public void close() throws Exception {
        entryChattingAiDatabaseListener.close();
        groupChattingAiDatabaseListener.close();
        mvStoreChatHistoryRepository.close();
    }
}
