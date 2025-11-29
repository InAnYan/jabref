package org.jabref.logic.ai;

import org.jabref.logic.ai.chatting.ChattingDatabaseListeners;
import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.GroupChatHistoryRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.SummariesService;
import org.jabref.logic.ai.summarization.SummarizationDatabaseListeners;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.database.BibDatabaseContext;

public class AiDatabaseListeners {
    private final ChattingDatabaseListeners chattingDatabaseListeners;
    private final SummarizationDatabaseListeners summarizationDatabaseListeners;

    public AiDatabaseListeners(
            EntryChatHistoryRepository entryChatHistoryRepository,
            GroupChatHistoryRepository groupChatHistoryRepository,
            AiPreferences aiPreferences,
            SummariesService summariesService,
            Summarizator summarizator
    ) {
        this.chattingDatabaseListeners = new ChattingDatabaseListeners(entryChatHistoryRepository, groupChatHistoryRepository);
        this.summarizationDatabaseListeners = new SummarizationDatabaseListeners(aiPreferences, summariesService, summarizator);
    }

    public void setupDatabase(BibDatabaseContext databaseContext) {
        chattingDatabaseListeners.setupDatabase(databaseContext);
        summarizationDatabaseListeners.setupDatabase(databaseContext);
    }
}
