package org.jabref.logic.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatHistoryService;
import org.jabref.logic.ai.chatting.CurrentlySelectedChatLanguageModel;
import org.jabref.logic.ai.chatting.storages.MVStoreChatHistoryRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.CurrentlySelectedEmbeddingModel;
import org.jabref.logic.ai.rag.IngestionService;
import org.jabref.logic.ai.rag.storages.MVStoreEmbeddingStore;
import org.jabref.logic.ai.rag.storages.MVStoreFullyIngestedDocumentsRepository;
import org.jabref.logic.ai.summarization.SummariesService;
import org.jabref.logic.ai.summarization.storages.MVStoreSummariesRepository;
import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
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
    public static final String VERSION = "1";

    private static final String EMBEDDINGS_FILE_NAME = "embeddings.mv";
    private static final String FULLY_INGESTED_FILE_NAME = "fully-ingested.mv";
    private static final String SUMMARIES_FILE_NAME = "summaries.mv";
    private static final String CHAT_HISTORY_FILE_NAME = "chat-histories.mv";

    // This field is used to shut down AI-related background tasks.
    // If a background task processes a big document and has a loop, then the task should check the status
    // of this property for being true. If it's true, then it should abort the cycle.
    private final BooleanProperty shutdownSignal = new SimpleBooleanProperty(false);

    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-retrieval-pool-%d").build()
    );

    private final MVStoreChatHistoryRepository mvStoreChatHistoryStorage;
    private final MVStoreEmbeddingStore mvStoreEmbeddingStore;
    private final MVStoreFullyIngestedDocumentsRepository mvStoreFullyIngestedDocumentsTracker;
    private final MVStoreSummariesRepository mvStoreSummariesStorage;

    private final AiTemplatesService templatesService;
    private final ChatHistoryService chatHistoryService;
    private final CurrentlySelectedChatLanguageModel currentlySelectedChatLanguageModel;
    private final CurrentlySelectedEmbeddingModel currentlySelectedEmbeddingModel;
    private final IngestionService ingestionService;
    private final SummariesService summariesService;

    public AiService(AiPreferences aiPreferences,
                     FilePreferences filePreferences,
                     CitationKeyPatternPreferences citationKeyPatternPreferences,
                     NotificationService notificationService,
                     TaskExecutor taskExecutor
    ) {

        this.mvStoreChatHistoryStorage = new MVStoreChatHistoryRepository(Directories.getAiFilesDirectory().resolve(CHAT_HISTORY_FILE_NAME), notificationService);
        this.mvStoreEmbeddingStore = new MVStoreEmbeddingStore(Directories.getAiFilesDirectory().resolve(EMBEDDINGS_FILE_NAME), notificationService);
        this.mvStoreFullyIngestedDocumentsTracker = new MVStoreFullyIngestedDocumentsRepository(Directories.getAiFilesDirectory().resolve(FULLY_INGESTED_FILE_NAME), notificationService);
        this.mvStoreSummariesStorage = new MVStoreSummariesRepository(Directories.getAiFilesDirectory().resolve(SUMMARIES_FILE_NAME), notificationService);

        this.templatesService = new AiTemplatesService(aiPreferences);
        this.chatHistoryService = new ChatHistoryService(citationKeyPatternPreferences, mvStoreChatHistoryStorage);
        this.currentlySelectedChatLanguageModel = new CurrentlySelectedChatLanguageModel(aiPreferences);
        this.currentlySelectedEmbeddingModel = new CurrentlySelectedEmbeddingModel(aiPreferences, notificationService, taskExecutor);

        this.ingestionService = new IngestionService(
                aiPreferences,
                shutdownSignal,
                currentlySelectedEmbeddingModel,
                mvStoreEmbeddingStore,
                mvStoreFullyIngestedDocumentsTracker,
                filePreferences,
                taskExecutor
        );

        this.summariesService = new SummariesService(
                aiPreferences,
                mvStoreSummariesStorage,
                currentlySelectedChatLanguageModel,
                templatesService,
                shutdownSignal,
                filePreferences,
                taskExecutor
        );
    }

    public CurrentlySelectedChatLanguageModel getChatLanguageModel() {
        return currentlySelectedChatLanguageModel;
    }

    public CurrentlySelectedEmbeddingModel getEmbeddingModel() {
        return currentlySelectedEmbeddingModel;
    }

    public ChatHistoryService getChatHistoryService() {
        return chatHistoryService;
    }

    public IngestionService getIngestionService() {
        return ingestionService;
    }

    public SummariesService getSummariesService() {
        return summariesService;
    }

    public AiTemplatesService getTemplatesService() {
        return templatesService;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return mvStoreEmbeddingStore;
    }

    public void setupDatabase(BibDatabaseContext context) {
        chatHistoryService.setupDatabase(context);
        ingestionService.setupDatabase(context);
        summariesService.setupDatabase(context);
    }

    @Override
    public void close() {
        shutdownSignal.set(true);

        cachedThreadPool.shutdownNow();
        currentlySelectedChatLanguageModel.close();
        currentlySelectedEmbeddingModel.close();

        mvStoreFullyIngestedDocumentsTracker.close();
        mvStoreEmbeddingStore.close();
        mvStoreSummariesStorage.close();
    }
}
