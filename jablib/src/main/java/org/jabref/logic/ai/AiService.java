package org.jabref.logic.ai;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.MVStoreChatHistoryRepository;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.embedding.MVStoreEmbeddingStore;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.listeners.GenerateEmbeddingsAiDatabaseListener;
import org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.repositories.MVStoreIngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.InMemorySummaryCache;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.listeners.GenerateSummaryAiDatabaseListener;
import org.jabref.logic.ai.summarization.repositories.MVStoreSummariesRepository;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

/// The main class for the AI functionality.
/// Holds all the AI components: LLM and embedding model, chat history and embedding cache.
public class AiService implements AutoCloseable {
    public static final String VERSION = "2";

    private static final String CHAT_HISTORY_FILE_NAME = "chat-histories.mv";
    private static final String EMBEDDINGS_FILE_NAME = "embeddings.mv";
    private static final String FULLY_INGESTED_FILE_NAME = "fully-ingested.mv";
    private static final String SUMMARIES_FILE_NAME = "summaries.mv";

    // Chatting components
    private final MVStoreChatHistoryRepository mvStoreChatHistoryRepository;
    private final InMemoryChatHistoryCache inMemoryChatHistoryCache;

    // Ingestion components
    private final EmbeddingModelCache embeddingModelCache;
    private final MVStoreEmbeddingStore mvStoreEmbeddingStore;
    private final MVStoreIngestedDocumentsRepository mvStoreIngestedDocumentsRepository;
    private final IngestionTaskAggregator ingestionTaskAggregator;
    private final EmbeddingsCleaner embeddingsCleaner;
    private final GenerateEmbeddingsAiDatabaseListener generateEmbeddingsAiDatabaseListener;

    // Summarization components
    private final MVStoreSummariesRepository mvStoreSummariesRepository;
    private final InMemorySummaryCache inMemorySummaryCache;
    private final SummarizationTaskAggregator summarizationTaskAggregator;
    private final GenerateSummaryAiDatabaseListener generateSummaryAiDatabaseListener;

    public AiService(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor
    ) {
        // Chatting components
        this.mvStoreChatHistoryRepository = new MVStoreChatHistoryRepository(
                Directories.getAiFilesDirectory().resolve(CHAT_HISTORY_FILE_NAME),
                notificationService
        );
        this.inMemoryChatHistoryCache = new InMemoryChatHistoryCache(mvStoreChatHistoryRepository);

        // Ingestion components
        this.embeddingModelCache = new EmbeddingModelCache(notificationService, taskExecutor);
        this.mvStoreEmbeddingStore = new MVStoreEmbeddingStore(
                Directories.getAiFilesDirectory().resolve(EMBEDDINGS_FILE_NAME),
                notificationService
        );
        this.mvStoreIngestedDocumentsRepository = new MVStoreIngestedDocumentsRepository(
                notificationService,
                Directories.getAiFilesDirectory().resolve(FULLY_INGESTED_FILE_NAME)
        );
        this.ingestionTaskAggregator = new IngestionTaskAggregator(taskExecutor);
        this.embeddingsCleaner = new EmbeddingsCleaner(
                aiPreferences,
                mvStoreEmbeddingStore,
                mvStoreIngestedDocumentsRepository
        );
        this.generateEmbeddingsAiDatabaseListener = new GenerateEmbeddingsAiDatabaseListener(
                aiPreferences,
                filePreferences,
                mvStoreIngestedDocumentsRepository,
                mvStoreEmbeddingStore,
                embeddingModelCache,
                ingestionTaskAggregator
        );

        // Summarization components
        this.mvStoreSummariesRepository = new MVStoreSummariesRepository(
                notificationService,
                Directories.getAiFilesDirectory().resolve(SUMMARIES_FILE_NAME)
        );
        this.inMemorySummaryCache = new InMemorySummaryCache(mvStoreSummariesRepository);
        this.summarizationTaskAggregator = new SummarizationTaskAggregator(taskExecutor, inMemorySummaryCache);
        this.generateSummaryAiDatabaseListener = new GenerateSummaryAiDatabaseListener(
                aiPreferences,
                filePreferences,
                summarizationTaskAggregator
        );
    }

    public void setupDatabase(BibDatabaseContext context) {
        generateEmbeddingsAiDatabaseListener.setupDatabase(context);
        generateSummaryAiDatabaseListener.setupDatabase(context);
    }

    public ChatHistoryRepository getChatHistoryRepository() {
        return mvStoreChatHistoryRepository;
    }

    public InMemoryChatHistoryCache getChatHistoryCache() {
        return inMemoryChatHistoryCache;
    }

    public EmbeddingModelCache getEmbeddingModelCache() {
        return embeddingModelCache;
    }

    public EmbeddingStore<TextSegment> getEmbeddingsStore() {
        return mvStoreEmbeddingStore;
    }

    public IngestedDocumentsRepository getIngestedDocumentsRepository() {
        return mvStoreIngestedDocumentsRepository;
    }

    public IngestionTaskAggregator getIngestionTaskAggregator() {
        return ingestionTaskAggregator;
    }

    public EmbeddingsCleaner getEmbeddingsCleaner() {
        return embeddingsCleaner;
    }

    public SummariesRepository getSummariesRepository() {
        return mvStoreSummariesRepository;
    }

    public InMemorySummaryCache getSummaryCache() {
        return inMemorySummaryCache;
    }

    public SummarizationTaskAggregator getSummarizationTaskAggregator() {
        return summarizationTaskAggregator;
    }

    @Override
    public void close() throws Exception {
        // Close listeners
        generateSummaryAiDatabaseListener.close();
        generateEmbeddingsAiDatabaseListener.close();

        // Flush caches to repositories (chat history handles smart transfers)
        inMemoryChatHistoryCache.close();
        inMemorySummaryCache.close();

        // Close embedding model cache
        embeddingModelCache.close();

        // Close repositories
        mvStoreSummariesRepository.close();
        mvStoreChatHistoryRepository.close();
        mvStoreEmbeddingStore.close();
        mvStoreIngestedDocumentsRepository.close();
    }
}
