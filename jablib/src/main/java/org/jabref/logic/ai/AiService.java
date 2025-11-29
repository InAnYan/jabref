package org.jabref.logic.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.EntryChattingDatabaseListener;
import org.jabref.logic.ai.chatting.GroupChattingDatabaseListener;
import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepositoryV2;
import org.jabref.logic.ai.chatting.repositories.GroupChatHistoryRepositoryV2;
import org.jabref.logic.ai.chatting.repositories.MVStoreChatHistoryRepositoryV1;
import org.jabref.logic.ai.chatting.repositories.MVStoreEntryChatHistoryRepositoryV2;
import org.jabref.logic.ai.chatting.repositories.MVStoreGroupChatHistoryRepositoryV2;
import org.jabref.logic.ai.current.CurrentAiTemplates;
import org.jabref.logic.ai.current.CurrentAnswerEngine;
import org.jabref.logic.ai.current.CurrentChatLanguageModel;
import org.jabref.logic.ai.current.CurrentDocumentSplitter;
import org.jabref.logic.ai.current.CurrentEmbeddingModel;
import org.jabref.logic.ai.current.CurrentSummarizator;
import org.jabref.logic.ai.current.CurrentTokenEstimator;
import org.jabref.logic.ai.customimplementations.embeddingstores.MVStoreEmbeddingStore;
import org.jabref.logic.ai.pipeline.IngestionService;
import org.jabref.logic.ai.pipeline.repositories.MVStoreIngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.SummariesService;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.repositories.MVStoreSummariesRepository;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * The main class for the AI functionality.
 * <p>
 * Holds all the AI components: LLM and embedding model, chat history and embeddings cache.
 */
public class AiService implements AutoCloseable {
    public static final String VERSION = "2";

    private static final String EMBEDDINGS_FILE_NAME = "embeddings.mv";
    private static final String FULLY_INGESTED_FILE_NAME = "fully-ingested.mv";
    private static final String SUMMARIES_FILE_NAME = "summaries.mv";
    private static final String CHAT_HISTORY_FILE_NAME = "chat-histories.mv"; // v1
    private static final String ENTRY_CHAT_HISTORY_FILE_NAME = "entries-chat-histories.mv"; // v2
    private static final String GROUP_CHAT_HISTORY_FILE_NAME = "groups-chat-histories.mv"; // v2

    // This field is used to shut down AI-related background tasks.
    // If a background task processes a big document and has a loop, then the task should check the status
    // of this property for being true. If it's true, then it should abort the cycle.
    private final BooleanProperty shutdownSignal = new SimpleBooleanProperty(false);

    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-retrieval-pool-%d").build()
    );

    private final MVStoreChatHistoryRepositoryV1 mvStoreChatHistoryStorage;

    private final MVStoreEntryChatHistoryRepositoryV2 mvStoreEntryChatHistoryStorage;
    private final MVStoreGroupChatHistoryRepositoryV2 mvStoreGroupChatHistoryStorage;

    private final MVStoreEmbeddingStore mvStoreEmbeddingStore;
    private final MVStoreIngestedDocumentsRepository mvStoreFullyIngestedDocumentsTracker;
    private final MVStoreSummariesRepository mvStoreSummariesStorage;

    private final CurrentAiTemplates currentAiTemplates;
    private final CurrentDocumentSplitter currentDocumentSplitter;
    private final CurrentTokenEstimator currentTokenEstimator;
    private final CurrentChatLanguageModel currentChatLanguageModel;
    private final CurrentEmbeddingModel currentEmbeddingModel;
    private final CurrentSummarizator currentSummarizator;
    private final CurrentAnswerEngine currentAnswerEngine;

    private final IngestionService ingestionService;
    private final SummariesService summariesService;

    private final EntryChattingDatabaseListener entryChattingDatabaseListener;
    private final GroupChattingDatabaseListener groupChattingDatabaseListener;

    public AiService(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor
    ) {

        this.mvStoreChatHistoryStorage = new MVStoreChatHistoryRepositoryV1(notificationService, Directories.getAiFilesDirectory().resolve(CHAT_HISTORY_FILE_NAME));

        this.mvStoreEntryChatHistoryStorage = new MVStoreEntryChatHistoryRepositoryV2(Directories.getAiFilesDirectory().resolve(ENTRY_CHAT_HISTORY_FILE_NAME), notificationService);
        this.mvStoreGroupChatHistoryStorage = new MVStoreGroupChatHistoryRepositoryV2(Directories.getAiFilesDirectory().resolve(GROUP_CHAT_HISTORY_FILE_NAME), notificationService);

        this.mvStoreEmbeddingStore = new MVStoreEmbeddingStore(Directories.getAiFilesDirectory().resolve(EMBEDDINGS_FILE_NAME), notificationService);
        this.mvStoreFullyIngestedDocumentsTracker = new MVStoreIngestedDocumentsRepository(notificationService, Directories.getAiFilesDirectory().resolve(FULLY_INGESTED_FILE_NAME));
        this.mvStoreSummariesStorage = new MVStoreSummariesRepository(notificationService, Directories.getAiFilesDirectory().resolve(SUMMARIES_FILE_NAME));

        this.currentAiTemplates = new CurrentAiTemplates(aiPreferences);
        this.currentDocumentSplitter = new CurrentDocumentSplitter(aiPreferences);
        this.currentTokenEstimator = new CurrentTokenEstimator(aiPreferences);
        this.currentChatLanguageModel = new CurrentChatLanguageModel(aiPreferences, currentTokenEstimator);
        this.currentEmbeddingModel = new CurrentEmbeddingModel(aiPreferences, notificationService, taskExecutor);
        this.currentSummarizator = new CurrentSummarizator(aiPreferences, currentAiTemplates);
        this.currentAnswerEngine = new CurrentAnswerEngine(aiPreferences, filePreferences, currentEmbeddingModel, mvStoreEmbeddingStore);

        this.ingestionService = new IngestionService(
                aiPreferences,
                filePreferences,
                taskExecutor,
                currentEmbeddingModel,
                mvStoreEmbeddingStore,
                currentDocumentSplitter,
                mvStoreFullyIngestedDocumentsTracker,
                shutdownSignal
        );

        this.summariesService = new SummariesService(
                aiPreferences,
                filePreferences,
                taskExecutor,
                currentChatLanguageModel,
                currentSummarizator,
                mvStoreSummariesStorage,
                shutdownSignal
        );

        this.entryChattingDatabaseListener = new EntryChattingDatabaseListener(mvStoreEntryChatHistoryStorage);
        this.groupChattingDatabaseListener = new GroupChattingDatabaseListener(mvStoreGroupChatHistoryStorage);
    }

    public CurrentDocumentSplitter getDocumentSplitter() {
        return currentDocumentSplitter;
    }

    public CurrentChatLanguageModel getChatLanguageModel() {
        return currentChatLanguageModel;
    }

    public CurrentEmbeddingModel getEmbeddingModel() {
        return currentEmbeddingModel;
    }

    public IngestionService getIngestionService() {
        return ingestionService;
    }

    public SummariesService getSummariesService() {
        return summariesService;
    }

    public CurrentAiTemplates getCurrentAiTemplates() {
        return currentAiTemplates;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return mvStoreEmbeddingStore;
    }

    public Summarizator getSummarizator() {
        return currentSummarizator;
    }

    public CurrentTokenEstimator getTokenEstimator() {
        return currentTokenEstimator;
    }

    public CurrentAnswerEngine getAnswerEngine() {
        return currentAnswerEngine;
    }

    public EntryChatHistoryRepositoryV2 getEntryChatHistoryRepository() {
        return mvStoreEntryChatHistoryStorage;
    }

    public GroupChatHistoryRepositoryV2 getGroupChatHistoryRepository() {
        return mvStoreGroupChatHistoryStorage;
    }

    public void setupDatabase(BibDatabaseContext context) {
        entryChattingDatabaseListener.setupDatabase(context);
        groupChattingDatabaseListener.setupDatabase(context);

        ingestionService.setupDatabase(context);
        summariesService.setupDatabase(context);
    }

    @Override
    public void close() throws Exception {
        shutdownSignal.set(true);

        cachedThreadPool.shutdownNow();
        currentChatLanguageModel.close();
        currentEmbeddingModel.close();

        mvStoreChatHistoryStorage.close();
        mvStoreFullyIngestedDocumentsTracker.close();
        mvStoreEmbeddingStore.close();
        mvStoreSummariesStorage.close();
    }
}
